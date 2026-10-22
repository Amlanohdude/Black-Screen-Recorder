package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.ads.NativeAdManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
* Immutable representation of all user entitlements and consumable usage credits.
*/
data class Entitlements(
val isPro: Boolean = false,
val customCoverCredits: Int = 0,
val customFolderExportCredits: Int = 0,
val autoStopCredits: Int = 0
)

/**
* Centralized Monetization & Entitlement Manager implementing the Consumable Premium-Use Model.
*
* Rules:
* 1. PRO is the ONLY permanent entitlement (unlimited features + ad removal).
* 2. Rewarded ads grant ONE consumable credit per view for a specific premium feature.
* 3. Consumable credits survive normal app restarts (persisted locally via SharedPreferences).
* 4. Consumptions are atomic and synchronized to prevent race conditions or double-decrement.
* 5. PRO users never consume credits.
*/
object EntitlementManager {
private const val PREFS_NAME = "bsr_entitlements"
private const val KEY_PRO = "entitlement_pro"
private const val KEY_CUSTOM_COVERS_CREDITS = "custom_cover_credits"
private const val KEY_CUSTOM_FOLDER_CREDITS = "custom_folder_export_credits"
private const val KEY_AUTO_STOP_CREDITS = "auto_stop_credits"

private var prefs: SharedPreferences? = null

private val _entitlements = MutableStateFlow(Entitlements())
val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()
val isProPurchased: StateFlow<Boolean>
    get() = MutableStateFlow(_entitlements.value.isPro).asStateFlow()

fun setProPurchased(purchased: Boolean) = grantPro(purchased)

/**
* Initializes the entitlement store from local persistent storage.
*/
fun init(context: Context) {
if (prefs == null) {
val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
prefs = p
_entitlements.value = Entitlements(
isPro = p.getBoolean(KEY_PRO, false),
customCoverCredits = p.getInt(KEY_CUSTOM_COVERS_CREDITS, 0),
customFolderExportCredits = p.getInt(KEY_CUSTOM_FOLDER_CREDITS, 0),
autoStopCredits = p.getInt(KEY_AUTO_STOP_CREDITS, 0)
)
}
}

/**
* Whether the user has permanent PRO active.
*/
fun isPro(): Boolean = _entitlements.value.isPro

/**
* Current available credits for each consumable feature.
*/
fun getCustomCoverCredits(): Int = _entitlements.value.customCoverCredits
fun getCustomFolderExportCredits(): Int = _entitlements.value.customFolderExportCredits
fun getAutoStopCredits(): Int = _entitlements.value.autoStopCredits

/**
* Check if a feature is permitted to run (either PRO or credits available).
*/
fun canUseCustomCovers(): Boolean = isPro() || _entitlements.value.customCoverCredits > 0
fun canUseCustomFolderExport(): Boolean = isPro() || _entitlements.value.customFolderExportCredits > 0
fun canUseAutoStop(): Boolean = isPro() || _entitlements.value.autoStopCredits > 0

// ==========================================
// CUSTOM COVERS CREDIT MANAGEMENT
// ==========================================

@Synchronized
fun addCustomCoverCredit(amount: Int = 1) {
if (amount <= 0) return
val newCredits = _entitlements.value.customCoverCredits + amount
prefs?.edit()?.putInt(KEY_CUSTOM_COVERS_CREDITS, newCredits)?.apply()
_entitlements.value = _entitlements.value.copy(customCoverCredits = newCredits)
}

@Synchronized
fun consumeCustomCoverCredit(): Boolean {
if (isPro()) return true
val current = _entitlements.value.customCoverCredits
if (current > 0) {
val updated = current - 1
prefs?.edit()?.putInt(KEY_CUSTOM_COVERS_CREDITS, updated)?.apply()
_entitlements.value = _entitlements.value.copy(customCoverCredits = updated)
return true
}
return false
}

// ==========================================
// CUSTOM FOLDER EXPORT CREDIT MANAGEMENT
// ==========================================

@Synchronized
fun addCustomFolderExportCredit(amount: Int = 1) {
if (amount <= 0) return
val newCredits = _entitlements.value.customFolderExportCredits + amount
prefs?.edit()?.putInt(KEY_CUSTOM_FOLDER_CREDITS, newCredits)?.apply()
_entitlements.value = _entitlements.value.copy(customFolderExportCredits = newCredits)
}

@Synchronized
fun consumeCustomFolderExportCredit(): Boolean {
if (isPro()) return true
val current = _entitlements.value.customFolderExportCredits
if (current > 0) {
val updated = current - 1
prefs?.edit()?.putInt(KEY_CUSTOM_FOLDER_CREDITS, updated)?.apply()
_entitlements.value = _entitlements.value.copy(customFolderExportCredits = updated)
return true
}
return false
}

// ==========================================
// AUTO STOP CREDIT MANAGEMENT
// ==========================================

@Synchronized
fun addAutoStopCredit(amount: Int = 1) {
if (amount <= 0) return
val newCredits = _entitlements.value.autoStopCredits + amount
prefs?.edit()?.putInt(KEY_AUTO_STOP_CREDITS, newCredits)?.apply()
_entitlements.value = _entitlements.value.copy(autoStopCredits = newCredits)
}

@Synchronized
fun consumeAutoStopCredit(): Boolean {
if (isPro()) return true
val current = _entitlements.value.autoStopCredits
if (current > 0) {
val updated = current - 1
prefs?.edit()?.putInt(KEY_AUTO_STOP_CREDITS, updated)?.apply()
_entitlements.value = _entitlements.value.copy(autoStopCredits = updated)
return true
}
return false
}

// ==========================================
// PRO ENTITLEMENT
// ==========================================

/**
* Grants or revokes full PRO entitlement.
* When granted, immediately destroys all loaded native ads throughout the app.
* Designed for future Google Play Billing purchase integration.
*/
fun grantPro(enabled: Boolean = true) {
prefs?.edit()?.putBoolean(KEY_PRO, enabled)?.apply()
_entitlements.value = _entitlements.value.copy(isPro = enabled)
if (enabled) {
NativeAdManager.destroyAll()
}
}

/**
* Reset entitlements for testing or development purposes.
*/
fun resetEntitlementsForTesting() {
prefs?.edit()?.clear()?.apply()
_entitlements.value = Entitlements()
}
}


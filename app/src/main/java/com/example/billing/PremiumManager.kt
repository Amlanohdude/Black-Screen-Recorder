package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import com.example.storage.EntitlementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for PRO entitlement across all distribution flavors.
 * Persists the user's permanent purchase status in local storage and exposes
 * an observable StateFlow<Boolean> for the UI and application components.
 */
object PremiumManager {
    private const val PREFS_NAME = "bsr_premium_entitlements"
    private const val KEY_IS_PRO = "is_pro_purchased"

    private var prefs: SharedPreferences? = null
    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs == null) {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sp
            val isProPersisted = sp.getBoolean(KEY_IS_PRO, false) || EntitlementManager.isPro()
            _isPro.value = isProPersisted
            if (isProPersisted && !EntitlementManager.isPro()) {
                EntitlementManager.grantPro(true)
            }
        }
    }

    /**
     * Synchronous check for PRO entitlement.
     */
    fun isPro(): Boolean = _isPro.value

    /**
     * Updates and persists the PRO status.
     * When set to true, unlocks all features and notifies EntitlementManager to destroy ads.
     */
    @Synchronized
    fun setPro(enabled: Boolean) {
        _isPro.value = enabled
        prefs?.edit()?.putBoolean(KEY_IS_PRO, enabled)?.commit()
        EntitlementManager.grantPro(enabled)
    }

    @androidx.annotation.VisibleForTesting
    fun resetForTesting() {
        prefs = null
        _isPro.value = false
    }
}

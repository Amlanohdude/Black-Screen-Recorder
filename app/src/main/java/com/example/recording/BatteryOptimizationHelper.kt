package com.example.recording

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {
    private const val TAG = "BatteryOptHelper"

    /**
     * Checks if the app is exempt from Android battery optimizations (Doze mode).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check battery optimization state: ${e.message}")
            false
        }
    }

    /**
     * Requests exemption from battery optimizations so long background recordings are not terminated.
     * Uses ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS with fallback to ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS not found, falling back to settings: ${e.message}")
            openBatteryOptimizationSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch battery optimization request: ${e.message}")
            openBatteryOptimizationSettings(context)
        }
    }

    /**
     * Opens the system battery optimization management page.
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings: ${e.message}")
            try {
                val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(appDetailsIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Identifies if the host device manufacturer is known for aggressive background process termination.
     */
    fun isAggressiveOem(manufacturer: String = Build.MANUFACTURER): Boolean {
        val lower = manufacturer.lowercase()
        return lower in listOf(
            "xiaomi", "redmi", "poco",
            "samsung",
            "huawei", "honor",
            "oppo", "vivo", "oneplus", "realme",
            "transsion", "infinix", "tecno"
        )
    }

    /**
     * Returns tailored background recording guidance for aggressive OEM devices.
     */
    fun getOemGuidance(manufacturer: String = Build.MANUFACTURER): String? {
        val lower = manufacturer.lowercase()
        return when {
            lower in listOf("xiaomi", "redmi", "poco") ->
                "On Xiaomi/MIUI devices, also enable Autostart in Security > Manage Apps > Black Screen Recorder."
            lower in listOf("samsung") ->
                "On Samsung devices, ensure the app is excluded from 'Deep sleeping apps' in Device Care > Battery."
            lower in listOf("oppo", "realme", "oneplus") ->
                "On ColorOS/OxygenOS, enable 'Allow background activity' and 'Auto launch' in App Management."
            lower in listOf("huawei", "honor") ->
                "On EMUI/MagicOS, set App Launch to 'Manage manually' with background activity enabled."
            else -> null
        }
    }
}

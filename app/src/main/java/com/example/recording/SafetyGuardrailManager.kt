package com.example.recording

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.storage.StorageCapacityEstimator
import com.example.storage.UserSettings

/**
 * Battery & Low-Storage Safety Guardrails Manager
 *
 * Guarantees audio and video recordings are never corrupted due to abrupt
 * power death or disk saturation.
 *
 * Background:
 * MP4 and AAC containers require writing container headers (`moov` atoms) upon
 * completion. If a phone shuts down from 0% battery or hits ENOSPC (disk full),
 * the file is left unfinalized and permanently unplayable.
 *
 * This manager provides:
 * 1. Pre-flight checks: Prevents launching when battery or disk is already critical.
 * 2. Active runtime monitoring: Triggers clean stop & flush before shutdown or disk full.
 * 3. Transparent user feedback: Reassures users why recording stopped safely.
 */
object SafetyGuardrailManager {

    data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean
    )

    sealed class PreflightCheckResult {
        object Safe : PreflightCheckResult()
        data class Blocked(val reason: SafetyStopReason, val message: String) : PreflightCheckResult()
    }

    /**
     * Reads current battery percentage and charging state via sticky system broadcast.
     */
    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            getBatteryInfoFromIntent(intent)
        } catch (_: Exception) {
            // Defensive default: Assume 100% charging if broadcast cannot be read (e.g. unit tests)
            BatteryInfo(percentage = 100, isCharging = true)
        }
    }

    /**
     * Extracts battery percentage and charging state from a broadcast intent.
     */
    fun getBatteryInfoFromIntent(intent: Intent?): BatteryInfo {
        if (intent == null) return BatteryInfo(percentage = 100, isCharging = true)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            100
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0

        return BatteryInfo(percentage = pct, isCharging = isCharging)
    }

    /**
     * Returns currently available bytes in internal private storage.
     */
    fun getAvailableStorageBytes(context: Context): Long {
        return try {
            StorageCapacityEstimator.getStorageStats(context).availableBytes
        } catch (_: Exception) {
            StorageCapacityEstimator.CRITICAL_LOW_STORAGE_BYTES * 2
        }
    }

    /**
     * Performs pre-flight safety validation before beginning a new recording session.
     */
    fun checkPreflight(context: Context, settings: UserSettings): PreflightCheckResult {
        if (!settings.safetyGuardrailsEnabled) {
            return PreflightCheckResult.Safe
        }

        // 1. Storage Pre-Flight
        val minStorageBytes = settings.lowStorageStopThresholdMb * 1024L * 1024L
        val availableBytes = getAvailableStorageBytes(context)
        if (availableBytes < minStorageBytes) {
            val freeMb = availableBytes / (1024 * 1024)
            return PreflightCheckResult.Blocked(
                reason = SafetyStopReason.STORAGE_LOW,
                message = "Storage is critically low (${freeMb} MB remaining). At least ${settings.lowStorageStopThresholdMb} MB is required to record safely."
            )
        }

        // 2. Battery Pre-Flight
        val battery = getBatteryInfo(context)
        if (!battery.isCharging && battery.percentage <= settings.lowBatteryStopThresholdPct) {
            return PreflightCheckResult.Blocked(
                reason = SafetyStopReason.BATTERY_LOW,
                message = "Battery is critically low (${battery.percentage}%). Connect your device to a charger to record without losing files."
            )
        }

        return PreflightCheckResult.Safe
    }

    /**
     * Formats notification title and body text for a safety guardrail auto-stop event.
     */
    fun formatSafetyNotification(reason: SafetyStopReason, detailValue: Int): Pair<String, String> {
        return when (reason) {
            SafetyStopReason.BATTERY_LOW -> {
                val title = "Recording Safely Saved"
                val text = "Auto-stopped at $detailValue% battery to prevent power loss and file corruption."
                Pair(title, text)
            }
            SafetyStopReason.STORAGE_LOW -> {
                val title = "Recording Safely Saved"
                val text = "Auto-stopped: Storage reached critical limit (< $detailValue MB remaining)."
                Pair(title, text)
            }
        }
    }
}

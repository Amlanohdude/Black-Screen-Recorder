package com.example.recording

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import com.example.storage.SettingsPreferences
import com.example.storage.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafetyGuardrailTest {

    private lateinit var context: Context
    private lateinit var prefs: SettingsPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = SettingsPreferences(context)
    }

    @Test
    fun batteryInfo_calculatesCorrectPercentageAndChargingStatus() {
        // Discharging at 3%
        val dischargingIntent = Intent().apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 3)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, 0)
        }
        val info1 = SafetyGuardrailManager.getBatteryInfoFromIntent(dischargingIntent)
        assertEquals(3, info1.percentage)
        assertFalse(info1.isCharging)

        // Charging via AC at 2%
        val chargingIntent = Intent().apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 2)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)
        }
        val info2 = SafetyGuardrailManager.getBatteryInfoFromIntent(chargingIntent)
        assertEquals(2, info2.percentage)
        assertTrue(info2.isCharging)
    }

    @Test
    fun batteryInfo_handlesNullIntentDefensively() {
        val info = SafetyGuardrailManager.getBatteryInfoFromIntent(null)
        assertEquals(100, info.percentage)
        assertTrue(info.isCharging)
    }

    @Test
    fun checkPreflight_allowsWhenGuardrailsDisabled() {
        val settings = UserSettings(
            safetyGuardrailsEnabled = false,
            lowBatteryStopThresholdPct = 10,
            lowStorageStopThresholdMb = 500000 // ridiculously high
        )
        val result = SafetyGuardrailManager.checkPreflight(context, settings)
        assertTrue(result is SafetyGuardrailManager.PreflightCheckResult.Safe)
    }

    @Test
    fun checkPreflight_blocksWhenStorageIsCriticallyLow() {
        // Request 1 Terabyte of free space to force storage block
        val settings = UserSettings(
            safetyGuardrailsEnabled = true,
            lowStorageStopThresholdMb = 1_000_000
        )
        val result = SafetyGuardrailManager.checkPreflight(context, settings)
        assertTrue(result is SafetyGuardrailManager.PreflightCheckResult.Blocked)
        val blocked = result as SafetyGuardrailManager.PreflightCheckResult.Blocked
        assertEquals(SafetyStopReason.STORAGE_LOW, blocked.reason)
        assertTrue(blocked.message.contains("Storage is critically low"))
    }

    @Test
    fun formatSafetyNotification_createsClearExplanatoryTexts() {
        val (batteryTitle, batteryText) = SafetyGuardrailManager.formatSafetyNotification(SafetyStopReason.BATTERY_LOW, 3)
        assertEquals("Recording Safely Saved", batteryTitle)
        assertTrue(batteryText.contains("3% battery"))

        val (storageTitle, storageText) = SafetyGuardrailManager.formatSafetyNotification(SafetyStopReason.STORAGE_LOW, 200)
        assertEquals("Recording Safely Saved", storageTitle)
        assertTrue(storageText.contains("200 MB remaining"))
    }

    @Test
    fun settingsPreferences_safetyGuardrailDefaultsAndUpdates() {
        val initialSettings = prefs.settings.value
        assertTrue(initialSettings.safetyGuardrailsEnabled)
        assertEquals(3, initialSettings.lowBatteryStopThresholdPct)
        assertEquals(200, initialSettings.lowStorageStopThresholdMb)

        prefs.updateSafetyGuardrailsEnabled(false)
        assertFalse(prefs.settings.value.safetyGuardrailsEnabled)

        prefs.updateLowBatteryStopThresholdPct(5)
        assertEquals(5, prefs.settings.value.lowBatteryStopThresholdPct)

        prefs.updateLowStorageStopThresholdMb(500)
        assertEquals(500, prefs.settings.value.lowStorageStopThresholdMb)

        // Restore defaults
        prefs.updateSafetyGuardrailsEnabled(true)
        prefs.updateLowBatteryStopThresholdPct(3)
        prefs.updateLowStorageStopThresholdMb(200)
    }
}

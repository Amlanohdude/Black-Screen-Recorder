package com.example.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOptimizationHelperTest {

    @Test
    fun isAggressiveOem_forXiaomi_returnsTrue() {
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("Xiaomi"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("redmi"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("POCO"))
    }

    @Test
    fun isAggressiveOem_forSamsung_returnsTrue() {
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("samsung"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("SAMSUNG"))
    }

    @Test
    fun isAggressiveOem_forOnePlusAndOppo_returnsTrue() {
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("OnePlus"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("OPPO"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("Realme"))
        assertTrue(BatteryOptimizationHelper.isAggressiveOem("vivo"))
    }

    @Test
    fun isAggressiveOem_forGooglePixel_returnsFalse() {
        assertFalse(BatteryOptimizationHelper.isAggressiveOem("Google"))
    }

    @Test
    fun getOemGuidance_forXiaomi_providesAutostartInstructions() {
        val guidance = BatteryOptimizationHelper.getOemGuidance("Xiaomi")
        assertNotNull(guidance)
        assertTrue(guidance!!.contains("Autostart"))
    }

    @Test
    fun getOemGuidance_forSamsung_providesDeepSleepingInstructions() {
        val guidance = BatteryOptimizationHelper.getOemGuidance("Samsung")
        assertNotNull(guidance)
        assertTrue(guidance!!.contains("Deep sleeping apps"))
    }

    @Test
    fun getOemGuidance_forPixel_returnsNull() {
        val guidance = BatteryOptimizationHelper.getOemGuidance("Google")
        assertNull(guidance)
    }
}

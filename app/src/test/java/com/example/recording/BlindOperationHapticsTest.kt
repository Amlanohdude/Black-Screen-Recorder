package com.example.recording

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlindOperationHapticsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun hapticConstants_areConfiguredCorrectly() {
        assertEquals(110L, BlindOperationHaptics.STARTED_DURATION_MS)
        assertEquals(400L, BlindOperationHaptics.SAVED_DURATION_MS)
        assertArrayEquals(longArrayOf(0L, 85L, 140L, 85L), BlindOperationHaptics.PAUSED_PATTERN_MS)
    }

    @Test
    fun pausedPattern_providesDistinctTwoPulseCadence() {
        val pattern = BlindOperationHaptics.PAUSED_PATTERN_MS
        // Pattern format: [delay, on1, off, on2]
        assertEquals(4, pattern.size)
        val initialDelay = pattern[0]
        val firstPulse = pattern[1]
        val pauseInterval = pattern[2]
        val secondPulse = pattern[3]

        assertEquals(0L, initialDelay)
        assertTrue("First pulse must be between 60ms and 120ms", firstPulse in 60L..120L)
        assertTrue("Pause interval must be at least 120ms to allow motor spin-down", pauseInterval >= 120L)
        assertTrue("Second pulse must be between 60ms and 120ms", secondPulse in 60L..120L)
    }

    @Test
    fun vibrateMethods_executeWithoutCrashing() {
        BlindOperationHaptics.vibrateStarted(context)
        BlindOperationHaptics.vibratePaused(context)
        BlindOperationHaptics.vibrateSaved(context)
    }
}

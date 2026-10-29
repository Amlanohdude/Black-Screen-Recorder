package com.example.recording

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Discreet "Blind Operation" Haptic Feedback Engine
 *
 * Provides tactile feedback for confident blind operation (e.g. phone in pocket,
 * face down, or held at waist level):
 * - 1 crisp vibration: Recording started successfully.
 * - 2 short vibrations: Recording paused.
 * - 1 long smooth vibration: Recording safely saved.
 */
object BlindOperationHaptics {

    private const val TAG = "BlindOperationHaptics"

    // Tactile vibration constants engineered for blind pocket/hand operation:
    // - 1 crisp: 110ms punchy pulse (replaces micro-click which is imperceptible in hand/pocket)
    // - 2 short: 85ms on, 140ms off, 85ms on (ample motor spin-down so 2 pulses are clearly felt)
    // - 1 long: 400ms solid reassuring vibration
    const val STARTED_DURATION_MS = 110L
    val PAUSED_PATTERN_MS = longArrayOf(0L, 85L, 140L, 85L)
    const val SAVED_DURATION_MS = 400L

    /**
     * 1 crisp vibration: Recording started successfully.
     * Snappy, punchy tactile pulse (110ms) easily perceptible in hand or pocket.
     */
    fun vibrateStarted(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(STARTED_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(STARTED_DURATION_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibrateStarted", e)
        }
    }

    /**
     * 2 short vibrations: Recording paused.
     * Double tactile pulse (85ms on, 140ms off, 85ms on) with sufficient spin-down
     * time so human touch distinctly perceives two separate pulses.
     */
    fun vibratePaused(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(PAUSED_PATTERN_MS, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(PAUSED_PATTERN_MS, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibratePaused", e)
        }
    }

    /**
     * 1 long smooth vibration: Recording safely saved.
     * Reassuring solid pulse (400ms) confirming file is verified on disk and database.
     */
    fun vibrateSaved(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(SAVED_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(SAVED_DURATION_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibrateSaved", e)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }
}

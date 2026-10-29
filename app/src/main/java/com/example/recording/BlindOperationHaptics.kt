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

    /**
     * 1 crisp vibration: Recording started successfully.
     * Crisp, punchy tactile pulse (~75ms).
     */
    fun vibrateStarted(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    return
                } catch (_: Exception) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(75L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(75L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibrateStarted", e)
        }
    }

    /**
     * 2 short vibrations: Recording paused.
     * Double tactile pulse (50ms on, 70ms off, 50ms on).
     */
    fun vibratePaused(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            val pattern = longArrayOf(0, 50, 70, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibratePaused", e)
        }
    }

    /**
     * 1 long smooth vibration: Recording safely saved.
     * Reassuring solid pulse (~380ms) confirming file is verified on disk and database.
     */
    fun vibrateSaved(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(380L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(380L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error performing vibrateSaved", e)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }
}

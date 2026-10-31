package com.example.recording

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Sensor-based no-touch recording stop detector.
 *
 * Supports:
 * 1. Shake to Stop: Deliberate multi-directional shake detection (filtering out normal walking/pocket jostling).
 * 2. Flip Face-Down to Stop: Detects when the device is inverted / placed flat face-down.
 */
class NonTouchStopSensorManager(
    private val context: Context,
    private val shakeEnabled: Boolean,
    private val flipEnabled: Boolean,
    private val onStopTriggered: () -> Unit
) : SensorEventListener {

    companion object {
        private const val TAG = "NonTouchStopSensor"
        private const val SHAKE_THRESHOLD_G = 2.4f
        private const val SHAKE_WINDOW_MS = 650L
        private const val FLIP_HOLD_MS = 400L
        private const val DEBOUNCE_MS = 1500L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakePeakTime = 0L
    private var shakePeakCount = 0
    private var faceDownStartTime = 0L
    private var lastTriggerTime = 0L

    fun start() {
        if (isListening || accelerometer == null || (!shakeEnabled && !flipEnabled)) return
        try {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
            lastShakePeakTime = 0L
            shakePeakCount = 0
            faceDownStartTime = 0L
            Log.d(TAG, "NonTouchStopSensorManager started (shake=$shakeEnabled, flip=$flipEnabled)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start accelerometer listener", e)
        }
    }

    fun stop() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(this)
            isListening = false
            Log.d(TAG, "NonTouchStopSensorManager stopped")
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < DEBOUNCE_MS) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // 1. Shake to Stop
        if (shakeEnabled) {
            val gX = ax / SensorManager.GRAVITY_EARTH
            val gY = ay / SensorManager.GRAVITY_EARTH
            val gZ = az / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD_G) {
                if (now - lastShakePeakTime < SHAKE_WINDOW_MS) {
                    shakePeakCount++
                    if (shakePeakCount >= 2) {
                        Log.i(TAG, "Deliberate shake gesture detected. Triggering stop.")
                        lastTriggerTime = now
                        shakePeakCount = 0
                        onStopTriggered()
                        return
                    }
                } else {
                    shakePeakCount = 1
                }
                lastShakePeakTime = now
            } else if (now - lastShakePeakTime > SHAKE_WINDOW_MS) {
                shakePeakCount = 0
            }
        }

        // 2. Flip Face-Down to Stop
        if (flipEnabled) {
            val gZ = az / SensorManager.GRAVITY_EARTH
            // Screen facing down means negative Z gravity approaching -1G
            val isFaceDown = gZ < -0.80f && abs(ax) < 3.5f && abs(ay) < 3.5f

            if (isFaceDown) {
                if (faceDownStartTime == 0L) {
                    faceDownStartTime = now
                } else if (now - faceDownStartTime >= FLIP_HOLD_MS) {
                    Log.i(TAG, "Flip face-down gesture detected. Triggering stop.")
                    lastTriggerTime = now
                    faceDownStartTime = 0L
                    onStopTriggered()
                    return
                }
            } else {
                faceDownStartTime = 0L
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

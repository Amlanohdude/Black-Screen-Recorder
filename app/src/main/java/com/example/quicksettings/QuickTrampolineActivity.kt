package com.example.quicksettings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.recording.BlindOperationHaptics
import com.example.recording.RecordingService
import com.example.storage.SettingsPreferences

/**
 * High-speed Trampoline Activity for Android 14+ While-In-Use FGS Compliance
 *
 * Android 14 (API 34+) strictly requires an app to be in a foreground activity state
 * when starting a Foreground Service of type 'camera' or 'microphone'.
 *
 * Quick Settings Tiles and App Widgets launch this lightweight, invisible, transient
 * Activity via startActivityAndCollapse() / startActivity(), which starts RecordingService
 * while legally in the foreground, and finishes instantly with zero visual flicker.
 */
class QuickTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetAction = intent?.action
        if (targetAction == RecordingService.ACTION_START_AUDIO || targetAction == RecordingService.ACTION_START_VIDEO) {
            val serviceIntent = Intent(this, RecordingService::class.java).apply {
                action = targetAction
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                val prefs = SettingsPreferences(applicationContext).settings.value
                if (prefs.hapticFeedbackEnabled) {
                    BlindOperationHaptics.vibrateStarted(applicationContext)
                }
                val isAudio = (targetAction == RecordingService.ACTION_START_AUDIO)
                val typeStr = if (isAudio) "Audio" else "Video"
                Toast.makeText(applicationContext, "$typeStr recording started (screen dark)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Could not start recording: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

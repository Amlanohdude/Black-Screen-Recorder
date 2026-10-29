package com.example.quicksettings

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.recording.RecordingService
import com.example.recording.RecordingType
import com.example.recording.SafetyGuardrailManager
import com.example.storage.SettingsPreferences

/**
 * Dedicated Quick Settings Audio Recording Tile
 *
 * Guarantees direct 1-tap audio recording without video camera activation.
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickAudioTileService : TileService() {

    companion object {
        private const val TAG = "QuickAudioTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val status = RecordingService.recordingStatus.value
        val isRecordingAudio = status.isRecording && status.type == RecordingType.AUDIO

        if (isRecordingAudio) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_subtitle_audio_recording)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_tap_to_stop)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_stop)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_quick_audio_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_tap_to_record)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_mic)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val status = RecordingService.recordingStatus.value

        if (status.isRecording) {
            val stopIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(stopIntent)
            updateTileState()
        } else {
            val hasMic = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasMic) {
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pending = android.app.PendingIntent.getActivity(
                        this,
                        0,
                        openIntent,
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pending)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(openIntent)
                }
                Toast.makeText(this, "Microphone permission required to record audio.", Toast.LENGTH_LONG).show()
                return
            }

            val prefs = SettingsPreferences(applicationContext).settings.value
            val preflight = SafetyGuardrailManager.checkPreflight(this, prefs)
            if (preflight is SafetyGuardrailManager.PreflightCheckResult.Blocked) {
                Toast.makeText(this, preflight.message, Toast.LENGTH_LONG).show()
                return
            }

            val launchIntent = Intent(this, QuickTrampolineActivity::class.java).apply {
                action = RecordingService.ACTION_START_AUDIO
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pending = PendingIntent.getActivity(
                        this,
                        1002,
                        launchIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pending)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(launchIntent)
                }
                updateTileState()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch QuickTrampolineActivity from QS audio tile", e)
            }
        }
    }
}

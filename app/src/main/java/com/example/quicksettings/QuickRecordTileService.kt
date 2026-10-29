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
import com.example.recording.SafetyGuardrailManager
import com.example.storage.SettingsPreferences

/**
 * Universal Quick Settings 1-Tap Recording Tile
 *
 * Allows users to trigger or stop discreet background recording directly
 * from Android's pull-down notification shade from anywhere (lockscreen or any app).
 *
 * Fully compliant with Android 14+ foreground service launch exemptions:
 * "User interaction with a Quick Settings tile owned by the app is an officially
 * recognized exemption to background service start restrictions."
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickRecordTileService : TileService() {

    companion object {
        private const val TAG = "QuickRecordTile"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val status = RecordingService.recordingStatus.value

        if (status.isRecording) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_subtitle_recording)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_tap_to_stop)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_stop)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_quick_record_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.tile_subtitle_tap_to_record)
            }
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_record)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val status = RecordingService.recordingStatus.value

        if (status.isRecording) {
            // STOP Active Recording
            val stopIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(stopIntent)
            updateTileState()
        } else {
            // START Recording
            val prefs = SettingsPreferences(applicationContext).settings.value
            val isAudio = (prefs.defaultRecordingType == "AUDIO")

            // 1. Permission Validation
            val hasMic = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val hasCamera = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasMic || (!isAudio && !hasCamera)) {
                // Open main screen to request permissions
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
                Toast.makeText(this, "Permissions required to record.", Toast.LENGTH_LONG).show()
                return
            }

            // 2. Safety Guardrails Pre-flight Check
            val preflight = SafetyGuardrailManager.checkPreflight(this, prefs)
            if (preflight is SafetyGuardrailManager.PreflightCheckResult.Blocked) {
                Toast.makeText(this, preflight.message, Toast.LENGTH_LONG).show()
                return
            }

            // 3. Launch via Trampoline Activity (guarantees foreground eligibility for camera/mic on Android 14+)
            val targetAction = if (isAudio) {
                RecordingService.ACTION_START_AUDIO
            } else {
                RecordingService.ACTION_START_VIDEO
            }
            val launchIntent = Intent(this, QuickTrampolineActivity::class.java).apply {
                action = targetAction
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pending = PendingIntent.getActivity(
                        this,
                        1001,
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
                Log.e(TAG, "Failed to launch QuickTrampolineActivity from QS tile", e)
            }
        }
    }
}

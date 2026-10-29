package com.example.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.recording.BlindOperationHaptics
import com.example.recording.RecordingService
import com.example.storage.SettingsPreferences

/**
 * 1-Tap Home Screen Widget for Instant Discreet Recording
 *
 * Provides a tactile, 1-tap toggle on the home screen:
 * - Tap to Start: Immediately begins background recording with 1 crisp vibration.
 * - Tap to Stop: Safely finalizes and saves the file with 1 long smooth vibration.
 * - Live State Synchronization: Automatically reflects active recording state
 *   even when triggered from inside the app or the notification shade.
 */
class QuickRecordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_RECORD) {
            handleToggleRecord(context)
        }
    }

    private fun handleToggleRecord(context: Context) {
        val currentStatus = RecordingService.recordingStatus.value
        val isRecording = currentStatus.isRecording

        if (isRecording) {
            // STOP RECORDING
            val stopIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            context.startService(stopIntent)
            val settings = SettingsPreferences(context).settings.value
            if (settings.hapticFeedbackEnabled) {
                BlindOperationHaptics.vibrateSaved(context)
            }
            Toast.makeText(context, "Recording stopped and saved", Toast.LENGTH_SHORT).show()
        } else {
            // START RECORDING
            val settings = SettingsPreferences(context).settings.value
            val isVideo = settings.defaultRecordingType.equals("VIDEO", ignoreCase = true)

            // Permission checks
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            if (isVideo && (!hasCamera || !hasMic)) {
                Toast.makeText(context, "Camera & microphone permissions required", Toast.LENGTH_LONG).show()
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(launchIntent)
                return
            } else if (!isVideo && !hasMic) {
                Toast.makeText(context, "Microphone permission required", Toast.LENGTH_LONG).show()
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(launchIntent)
                return
            }

            val startIntent = Intent(context, RecordingService::class.java).apply {
                action = if (isVideo) RecordingService.ACTION_START_VIDEO else RecordingService.ACTION_START_AUDIO
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }

            if (settings.hapticFeedbackEnabled) {
                BlindOperationHaptics.vibrateStarted(context)
            }
            val modeName = if (isVideo) "Video" else "Audio"
            Toast.makeText(context, "$modeName recording started (screen dark)", Toast.LENGTH_SHORT).show()
        }

        // Immediately update all widgets
        updateAllWidgets(context)
    }

    companion object {
        const val ACTION_TOGGLE_RECORD = "com.blackscreenrecorder.widget.ACTION_TOGGLE_RECORD"
        private const val TAG = "QuickRecordWidget"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_record)
            val currentStatus = RecordingService.recordingStatus.value
            val isRecording = currentStatus.isRecording

            if (isRecording) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_recording)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_stop)
                views.setTextViewText(R.id.widget_title, "RECORDING")
                views.setTextColor(R.id.widget_title, Color.parseColor("#FF453A"))
                views.setTextViewText(R.id.widget_subtitle, "Tap to Stop")
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#FF9F9B"))
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_idle)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_rec_idle)
                views.setTextViewText(R.id.widget_title, "Quick Record")
                views.setTextColor(R.id.widget_title, Color.parseColor("#F5F5F7"))
                views.setTextViewText(R.id.widget_subtitle, "Tap to Start")
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#8E8E93"))
            }

            // Click pending intent
            val toggleIntent = Intent(context, QuickRecordWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_RECORD
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                pendingFlags
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, QuickRecordWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating widgets", e)
            }
        }
    }
}

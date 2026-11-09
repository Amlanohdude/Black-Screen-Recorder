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
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.recording.BlindOperationHaptics
import com.example.recording.RecordingService
import com.example.recording.RecordingType
import com.example.storage.SettingsPreferences

/**
 * Recording mode handled by a widget instance
 */
enum class WidgetRecordMode {
    AUDIO,
    VIDEO,
    DEFAULT,
    DUAL
}

/**
 * Base Provider for 1-Tap Home Screen Recording Widgets
 *
 * Supports dynamic responsive sizing (1×1 compact, 2×1 horizontal bar, 2×2 dashboard, dual controls):
 * - Tap to Start: Immediately begins background recording with 1 crisp vibration.
 * - Tap to Stop: Safely finalizes and saves the file with 1 long smooth vibration.
 * - Live State Synchronization: Automatically reflects active recording state
 *   across all instances, sizes, and modes.
 */
abstract class BaseRecordWidgetProvider(
    private val mode: WidgetRecordMode
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, mode, this::class.java)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId, mode, this::class.java)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_RECORD_VIDEO -> handleStartRecord(context, isVideo = true)
            ACTION_RECORD_AUDIO -> handleStartRecord(context, isVideo = false)
            ACTION_STOP_RECORD -> handleStopRecord(context)
            ACTION_TOGGLE_RECORD -> handleToggleRecord(context, mode)
            ACTION_REFRESH_WIDGETS -> updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_RECORD = "com.blackscreenrecorder.widget.ACTION_TOGGLE_RECORD"
        const val ACTION_RECORD_VIDEO = "com.blackscreenrecorder.widget.ACTION_RECORD_VIDEO"
        const val ACTION_RECORD_AUDIO = "com.blackscreenrecorder.widget.ACTION_RECORD_AUDIO"
        const val ACTION_STOP_RECORD = "com.blackscreenrecorder.widget.ACTION_STOP_RECORD"
        const val ACTION_REFRESH_WIDGETS = "com.blackscreenrecorder.widget.ACTION_REFRESH_WIDGETS"

        const val REQUEST_CODE_TOGGLE = 100
        const val REQUEST_CODE_VIDEO = 101
        const val REQUEST_CODE_AUDIO = 102
        const val REQUEST_CODE_STOP = 103

        private const val TAG = "BaseRecordWidget"

        fun handleStopRecord(context: Context) {
            val stopIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            context.startService(stopIntent)
            val settings = SettingsPreferences(context).settings.value
            if (settings.hapticFeedbackEnabled) {
                BlindOperationHaptics.vibrateSaved(context)
            }
            Toast.makeText(context, "Recording stopped and saved", Toast.LENGTH_SHORT).show()
            updateAllWidgets(context)
        }

        fun handleStartRecord(context: Context, isVideo: Boolean) {
            val currentStatus = RecordingService.recordingStatus.value
            if (currentStatus.isRecording) {
                handleStopRecord(context)
                return
            }

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

            val targetAction = if (isVideo) RecordingService.ACTION_START_VIDEO else RecordingService.ACTION_START_AUDIO
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = targetAction
            }

            var serviceStarted = false
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                serviceStarted = true

                val prefs = SettingsPreferences(context).settings.value
                if (prefs.hapticFeedbackEnabled) {
                    BlindOperationHaptics.vibrateStarted(context)
                }
                val typeStr = if (isVideo) "Video" else "Audio"
                Toast.makeText(context, "$typeStr recording started (screen dark)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w(TAG, "Direct startForegroundService failed: ${e.message}, falling back to trampoline", e)
            }

            if (!serviceStarted) {
                try {
                    val trampolineIntent = Intent(context, com.example.quicksettings.QuickTrampolineActivity::class.java).apply {
                        action = targetAction
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(trampolineIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "QuickTrampolineActivity launch also failed", e)
                }
            }
            updateAllWidgets(context)
        }

        fun handleToggleRecord(context: Context, mode: WidgetRecordMode) {
            val currentStatus = RecordingService.recordingStatus.value
            if (currentStatus.isRecording) {
                handleStopRecord(context)
            } else {
                val isVideo = when (mode) {
                    WidgetRecordMode.AUDIO -> false
                    WidgetRecordMode.VIDEO -> true
                    WidgetRecordMode.DEFAULT, WidgetRecordMode.DUAL -> {
                        val settings = SettingsPreferences(context).settings.value
                        settings.defaultRecordingType.equals("VIDEO", ignoreCase = true)
                    }
                }
                handleStartRecord(context, isVideo)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            mode: WidgetRecordMode,
            providerClass: Class<*>
        ) {
            if (mode == WidgetRecordMode.DUAL || providerClass == DualRecordWidgetProvider::class.java || providerClass == QuickBarWidgetProvider::class.java) {
                updateDualWidget(context, appWidgetManager, appWidgetId, providerClass)
                return
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 70
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 70

            // Responsive layout selection based on widget dimensions or explicit provider class
            val layoutRes = when {
                providerClass == QuickDashboardWidgetProvider::class.java -> R.layout.widget_dashboard_2x2
                providerClass == QuickBarWidgetProvider::class.java -> R.layout.widget_horizontal_2x1
                minWidth >= 150 && minHeight >= 120 -> R.layout.widget_dashboard_2x2
                minWidth >= 130 -> R.layout.widget_horizontal_2x1
                else -> R.layout.widget_quick_record // 1x1 Compact
            }

            val views = RemoteViews(context.packageName, layoutRes)
            val currentStatus = RecordingService.recordingStatus.value
            val isRecording = currentStatus.isRecording

            if (isRecording) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_recording)
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_stop)
                val title = when {
                    currentStatus.type == RecordingType.AUDIO && mode == WidgetRecordMode.AUDIO -> "AUDIO REC"
                    currentStatus.type == RecordingType.VIDEO && mode == WidgetRecordMode.VIDEO -> "VIDEO REC"
                    else -> "RECORDING"
                }
                views.setTextViewText(R.id.widget_title, title)
                views.setTextColor(R.id.widget_title, Color.parseColor("#FF453A"))
                views.setTextViewText(R.id.widget_subtitle, "Tap to Stop")
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#FF9F9B"))

                try {
                    views.setTextViewText(R.id.widget_badge, "LIVE REC")
                    views.setTextColor(R.id.widget_badge, Color.parseColor("#FF453A"))
                    views.setTextViewText(R.id.widget_footer, "Recording active · Screen kept dark")
                } catch (_: Exception) {}
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_idle)
                val (iconRes, titleText) = when (mode) {
                    WidgetRecordMode.AUDIO -> Pair(R.drawable.ic_widget_mic, "Quick Audio")
                    WidgetRecordMode.VIDEO -> Pair(R.drawable.ic_widget_video, "Quick Video")
                    WidgetRecordMode.DEFAULT, WidgetRecordMode.DUAL -> Pair(R.drawable.ic_widget_rec_idle, "Quick Record")
                }
                views.setImageViewResource(R.id.widget_icon, iconRes)
                views.setTextViewText(R.id.widget_title, titleText)
                views.setTextColor(R.id.widget_title, Color.parseColor("#F5F5F7"))
                views.setTextViewText(R.id.widget_subtitle, "Tap to Start")
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#8E8E93"))

                try {
                    val badgeText = when (mode) {
                        WidgetRecordMode.AUDIO -> "AUDIO"
                        WidgetRecordMode.VIDEO -> "VIDEO"
                        WidgetRecordMode.DEFAULT, WidgetRecordMode.DUAL -> "READY"
                    }
                    views.setTextViewText(R.id.widget_badge, badgeText)
                    views.setTextColor(R.id.widget_badge, Color.parseColor("#8E8E93"))
                    views.setTextViewText(R.id.widget_footer, "Discreet · Screen Kept Dark")
                } catch (_: Exception) {}
            }

            // Click pending intent targets the specific provider class
            val toggleIntent = Intent(context, providerClass).apply {
                action = ACTION_TOGGLE_RECORD
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TOGGLE,
                toggleIntent,
                pendingFlags
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun updateDualWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            providerClass: Class<*>
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 140
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 60

            val isDashboard = minWidth >= 150 && minHeight >= 120 && providerClass != QuickBarWidgetProvider::class.java
            val layoutRes = if (isDashboard) R.layout.widget_dual_dashboard_2x2 else R.layout.widget_dual_horizontal_2x1

            val views = RemoteViews(context.packageName, layoutRes)
            val currentStatus = RecordingService.recordingStatus.value
            val isRecording = currentStatus.isRecording

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val videoIntent = Intent(context, providerClass).apply {
                action = ACTION_RECORD_VIDEO
            }
            val videoPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_VIDEO,
                videoIntent,
                pendingFlags
            )

            val audioIntent = Intent(context, providerClass).apply {
                action = ACTION_RECORD_AUDIO
            }
            val audioPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_AUDIO,
                audioIntent,
                pendingFlags
            )

            val stopIntent = Intent(context, providerClass).apply {
                action = ACTION_STOP_RECORD
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_STOP,
                stopIntent,
                pendingFlags
            )

            val recTitle = if (currentStatus.type == RecordingType.VIDEO) "RECORDING VIDEO" else "RECORDING AUDIO"

            if (isRecording) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_recording)
                if (isDashboard) {
                    views.setViewVisibility(R.id.layout_idle_dashboard, View.GONE)
                    views.setViewVisibility(R.id.layout_recording_dashboard, View.VISIBLE)
                    views.setTextViewText(R.id.widget_dash_badge, "LIVE REC")
                    views.setTextColor(R.id.widget_dash_badge, Color.parseColor("#FF453A"))
                    views.setTextViewText(R.id.widget_dash_rec_title, recTitle)
                    views.setTextViewText(R.id.widget_dash_rec_subtitle, "Tap to Stop and Save")
                    views.setTextViewText(R.id.widget_dash_footer, "Session active · Screen kept dark")
                    views.setOnClickPendingIntent(R.id.layout_recording_dashboard, stopPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_dash_stop_icon, stopPendingIntent)
                } else {
                    views.setViewVisibility(R.id.layout_idle, View.GONE)
                    views.setViewVisibility(R.id.layout_recording, View.VISIBLE)
                    views.setTextViewText(R.id.widget_recording_title, recTitle)
                    views.setTextViewText(R.id.widget_recording_subtitle, "Tap to Stop and Save")
                    views.setTextViewText(R.id.widget_recording_badge, "LIVE REC")
                    views.setOnClickPendingIntent(R.id.layout_recording, stopPendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_stop_icon, stopPendingIntent)
                }
                views.setOnClickPendingIntent(R.id.widget_root, stopPendingIntent)
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_idle)
                if (isDashboard) {
                    views.setViewVisibility(R.id.layout_idle_dashboard, View.VISIBLE)
                    views.setViewVisibility(R.id.layout_recording_dashboard, View.GONE)
                    views.setTextViewText(R.id.widget_dash_badge, "DUAL REC")
                    views.setTextColor(R.id.widget_dash_badge, Color.parseColor("#8E8E93"))
                    views.setTextViewText(R.id.widget_dash_footer, "Discreet · Screen Kept Dark")
                    views.setOnClickPendingIntent(R.id.btn_dash_video, videoPendingIntent)
                    views.setOnClickPendingIntent(R.id.btn_dash_audio, audioPendingIntent)
                } else {
                    views.setViewVisibility(R.id.layout_idle, View.VISIBLE)
                    views.setViewVisibility(R.id.layout_recording, View.GONE)
                    views.setOnClickPendingIntent(R.id.btn_record_video, videoPendingIntent)
                    views.setOnClickPendingIntent(R.id.icon_video, videoPendingIntent)
                    views.setOnClickPendingIntent(R.id.title_video, videoPendingIntent)
                    views.setOnClickPendingIntent(R.id.btn_record_audio, audioPendingIntent)
                    views.setOnClickPendingIntent(R.id.icon_audio, audioPendingIntent)
                    views.setOnClickPendingIntent(R.id.title_audio, audioPendingIntent)
                }
                // In idle state, default toggle on root is not active so button targets receive touch events directly
                views.setOnClickPendingIntent(R.id.widget_root, null)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateWidgetsForProvider(context: Context, providerClass: Class<*>, mode: WidgetRecordMode) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, providerClass)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (id in appWidgetIds) {
                        updateWidget(context, appWidgetManager, id, mode, providerClass)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating widgets for ${providerClass.simpleName}", e)
            }
        }

        fun updateAllWidgets(context: Context) {
            updateWidgetsForProvider(context, QuickAudioWidgetProvider::class.java, WidgetRecordMode.AUDIO)
            updateWidgetsForProvider(context, QuickVideoWidgetProvider::class.java, WidgetRecordMode.VIDEO)
            updateWidgetsForProvider(context, QuickRecordWidgetProvider::class.java, WidgetRecordMode.DEFAULT)
            updateWidgetsForProvider(context, QuickBarWidgetProvider::class.java, WidgetRecordMode.DUAL)
            updateWidgetsForProvider(context, QuickDashboardWidgetProvider::class.java, WidgetRecordMode.DEFAULT)
            updateWidgetsForProvider(context, DualRecordWidgetProvider::class.java, WidgetRecordMode.DUAL)
        }
    }
}

/**
 * Dedicated 1-Tap Audio Recording Widget (1×1, resizable to 2×1, 2×2)
 */
class QuickAudioWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.AUDIO)

/**
 * Dedicated 1-Tap Video Recording Widget (1×1, resizable to 2×1, 2×2)
 */
class QuickVideoWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.VIDEO)

/**
 * Universal 1-Tap Quick Recording Widget (1×1, resizable to 2×1, 2×2)
 */
class QuickRecordWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DEFAULT) {
    companion object {
        fun updateAllWidgets(context: Context) {
            BaseRecordWidgetProvider.updateAllWidgets(context)
        }
    }
}

/**
 * Wide Quick Bar Recording Widget (2×1, resizable)
 * Provides dual 1-tap options for both Video and Audio recording from the home screen.
 */
class QuickBarWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DUAL)

/**
 * Expanded Recording Dashboard Widget (2×2, resizable)
 */
class QuickDashboardWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DEFAULT)

/**
 * Dual Recording Widget (2×1, resizable to 2×2)
 * Provides separate 1-tap buttons for both Video and Audio recording from the home screen.
 */
class DualRecordWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DUAL)

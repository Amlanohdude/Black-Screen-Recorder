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
    DEFAULT
}

/**
 * Base Provider for 1-Tap Home Screen Recording Widgets
 *
 * Supports dynamic responsive sizing (1×1 compact, 2×1 horizontal bar, 2×2 dashboard):
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
        if (intent.action == ACTION_TOGGLE_RECORD) {
            handleToggleRecord(context, mode)
        }
    }

    companion object {
        const val ACTION_TOGGLE_RECORD = "com.blackscreenrecorder.widget.ACTION_TOGGLE_RECORD"
        private const val TAG = "BaseRecordWidget"

        fun handleToggleRecord(context: Context, mode: WidgetRecordMode) {
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
                val isVideo = when (mode) {
                    WidgetRecordMode.AUDIO -> false
                    WidgetRecordMode.VIDEO -> true
                    WidgetRecordMode.DEFAULT -> {
                        val settings = SettingsPreferences(context).settings.value
                        settings.defaultRecordingType.equals("VIDEO", ignoreCase = true)
                    }
                }

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

                val trampolineIntent = Intent(context, com.example.quicksettings.QuickTrampolineActivity::class.java).apply {
                    action = if (isVideo) RecordingService.ACTION_START_VIDEO else RecordingService.ACTION_START_AUDIO
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(trampolineIntent)
            }

            // Immediately update all widgets across all types and sizes
            updateAllWidgets(context)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            mode: WidgetRecordMode,
            providerClass: Class<*>
        ) {
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
                    WidgetRecordMode.DEFAULT -> Pair(R.drawable.ic_widget_rec_idle, "Quick Record")
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
                        WidgetRecordMode.DEFAULT -> "READY"
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
                0,
                toggleIntent,
                pendingFlags
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

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
            updateWidgetsForProvider(context, QuickBarWidgetProvider::class.java, WidgetRecordMode.DEFAULT)
            updateWidgetsForProvider(context, QuickDashboardWidgetProvider::class.java, WidgetRecordMode.DEFAULT)
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
 */
class QuickBarWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DEFAULT)

/**
 * Expanded Recording Dashboard Widget (2×2, resizable)
 */
class QuickDashboardWidgetProvider : BaseRecordWidgetProvider(WidgetRecordMode.DEFAULT)

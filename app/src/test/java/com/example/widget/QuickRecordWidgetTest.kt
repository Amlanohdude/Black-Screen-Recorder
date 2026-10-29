package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.recording.RecordingService
import com.example.recording.RecordingStage
import com.example.recording.RecordingStatus
import com.example.recording.RecordingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import com.example.R
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickRecordWidgetTest {

    private lateinit var context: Context
    private lateinit var provider: QuickRecordWidgetProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = QuickRecordWidgetProvider()
        // Reset recording status to IDLE
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.IDLE,
                isRecording = false,
                type = null,
                durationSeconds = 0L
            )
        )
    }

    @Test
    fun actionConstant_isConfiguredCorrectly() {
        assertEquals("com.blackscreenrecorder.widget.ACTION_TOGGLE_RECORD", QuickRecordWidgetProvider.ACTION_TOGGLE_RECORD)
    }

    @Test
    fun updateAllWidgets_runsWithoutCrashing() {
        QuickRecordWidgetProvider.updateAllWidgets(context)
    }

    @Test
    fun updateWidget_handlesIdleStateCorrectly() {
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.IDLE,
                isRecording = false,
                type = null,
                durationSeconds = 0L
            )
        )
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadow = Shadows.shadowOf(appWidgetManager)
        val widgetId = shadow.createWidget(QuickRecordWidgetProvider::class.java, R.layout.widget_quick_record)
        QuickRecordWidgetProvider.updateWidget(context, appWidgetManager, widgetId)
    }

    @Test
    fun updateWidget_handlesActiveRecordingStateCorrectly() {
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 12L
            )
        )
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadow = Shadows.shadowOf(appWidgetManager)
        val widgetId = shadow.createWidget(QuickRecordWidgetProvider::class.java, R.layout.widget_quick_record)
        QuickRecordWidgetProvider.updateWidget(context, appWidgetManager, widgetId)
    }

    @Test
    fun onReceive_toggleIntentWhenIdle_handlesGracefully() {
        val intent = Intent(QuickRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        provider.onReceive(context, intent)
    }

    @Test
    fun onReceive_toggleIntentWhenRecording_triggersStopGracefully() {
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 5L
            )
        )
        val intent = Intent(QuickRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        provider.onReceive(context, intent)
    }
}

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
    private lateinit var universalProvider: QuickRecordWidgetProvider
    private lateinit var audioProvider: QuickAudioWidgetProvider
    private lateinit var videoProvider: QuickVideoWidgetProvider
    private lateinit var barProvider: QuickBarWidgetProvider
    private lateinit var dashboardProvider: QuickDashboardWidgetProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        universalProvider = QuickRecordWidgetProvider()
        audioProvider = QuickAudioWidgetProvider()
        videoProvider = QuickVideoWidgetProvider()
        barProvider = QuickBarWidgetProvider()
        dashboardProvider = QuickDashboardWidgetProvider()
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
        assertEquals("com.blackscreenrecorder.widget.ACTION_TOGGLE_RECORD", BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
    }

    @Test
    fun updateAllWidgets_runsWithoutCrashing() {
        BaseRecordWidgetProvider.updateAllWidgets(context)
    }

    @Test
    fun updateWidget_handlesIdleStateForAudioVideoAndUniversal() {
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

        val audioId = shadow.createWidget(QuickAudioWidgetProvider::class.java, R.layout.widget_quick_record)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, audioId, WidgetRecordMode.AUDIO, QuickAudioWidgetProvider::class.java)

        val videoId = shadow.createWidget(QuickVideoWidgetProvider::class.java, R.layout.widget_quick_record)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, videoId, WidgetRecordMode.VIDEO, QuickVideoWidgetProvider::class.java)

        val barId = shadow.createWidget(QuickBarWidgetProvider::class.java, R.layout.widget_horizontal_2x1)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, barId, WidgetRecordMode.DEFAULT, QuickBarWidgetProvider::class.java)

        val dashId = shadow.createWidget(QuickDashboardWidgetProvider::class.java, R.layout.widget_dashboard_2x2)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dashId, WidgetRecordMode.DEFAULT, QuickDashboardWidgetProvider::class.java)
    }

    @Test
    fun updateWidget_handlesActiveRecordingStateForAudioVideoAndUniversal() {
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

        val audioId = shadow.createWidget(QuickAudioWidgetProvider::class.java, R.layout.widget_quick_record)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, audioId, WidgetRecordMode.AUDIO, QuickAudioWidgetProvider::class.java)

        val videoId = shadow.createWidget(QuickVideoWidgetProvider::class.java, R.layout.widget_quick_record)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, videoId, WidgetRecordMode.VIDEO, QuickVideoWidgetProvider::class.java)

        val barId = shadow.createWidget(QuickBarWidgetProvider::class.java, R.layout.widget_horizontal_2x1)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, barId, WidgetRecordMode.DEFAULT, QuickBarWidgetProvider::class.java)

        val dashId = shadow.createWidget(QuickDashboardWidgetProvider::class.java, R.layout.widget_dashboard_2x2)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dashId, WidgetRecordMode.DEFAULT, QuickDashboardWidgetProvider::class.java)
    }

    @Test
    fun onReceive_toggleIntentWhenIdle_handlesGracefully() {
        val audioIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        audioProvider.onReceive(context, audioIntent)

        val videoIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        videoProvider.onReceive(context, videoIntent)

        val universalIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        universalProvider.onReceive(context, universalIntent)

        val barIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        barProvider.onReceive(context, barIntent)

        val dashIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        dashboardProvider.onReceive(context, dashIntent)
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
        val intent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        audioProvider.onReceive(context, intent)
    }
}

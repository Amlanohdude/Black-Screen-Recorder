package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.recording.RecordingService
import com.example.recording.RecordingStage
import com.example.recording.RecordingStatus
import com.example.recording.RecordingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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
    private lateinit var dualProvider: DualRecordWidgetProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        universalProvider = QuickRecordWidgetProvider()
        audioProvider = QuickAudioWidgetProvider()
        videoProvider = QuickVideoWidgetProvider()
        barProvider = QuickBarWidgetProvider()
        dashboardProvider = QuickDashboardWidgetProvider()
        dualProvider = DualRecordWidgetProvider()

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
        assertEquals("com.blackscreenrecorder.widget.ACTION_RECORD_VIDEO", BaseRecordWidgetProvider.ACTION_RECORD_VIDEO)
        assertEquals("com.blackscreenrecorder.widget.ACTION_RECORD_AUDIO", BaseRecordWidgetProvider.ACTION_RECORD_AUDIO)
        assertEquals("com.blackscreenrecorder.widget.ACTION_STOP_RECORD", BaseRecordWidgetProvider.ACTION_STOP_RECORD)
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

        val barId = shadow.createWidget(QuickBarWidgetProvider::class.java, R.layout.widget_dual_horizontal_2x1)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, barId, WidgetRecordMode.DUAL, QuickBarWidgetProvider::class.java)

        val dashId = shadow.createWidget(QuickDashboardWidgetProvider::class.java, R.layout.widget_dashboard_2x2)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dashId, WidgetRecordMode.DEFAULT, QuickDashboardWidgetProvider::class.java)
    }

    @Test
    fun updateWidget_handlesDualWidgetHorizontalAndDashboardInIdleState() {
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

        // 2x1 Horizontal Dual Bar
        val dualBarId = shadow.createWidget(DualRecordWidgetProvider::class.java, R.layout.widget_dual_horizontal_2x1)
        val barOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
        }
        appWidgetManager.updateAppWidgetOptions(dualBarId, barOptions)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dualBarId, WidgetRecordMode.DUAL, DualRecordWidgetProvider::class.java)

        // 2x2 Dual Dashboard
        val dualDashId = shadow.createWidget(DualRecordWidgetProvider::class.java, R.layout.widget_dual_dashboard_2x2)
        val dashOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        }
        appWidgetManager.updateAppWidgetOptions(dualDashId, dashOptions)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dualDashId, WidgetRecordMode.DUAL, DualRecordWidgetProvider::class.java)
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

        val barId = shadow.createWidget(QuickBarWidgetProvider::class.java, R.layout.widget_dual_horizontal_2x1)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, barId, WidgetRecordMode.DUAL, QuickBarWidgetProvider::class.java)

        val dashId = shadow.createWidget(QuickDashboardWidgetProvider::class.java, R.layout.widget_dashboard_2x2)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dashId, WidgetRecordMode.DEFAULT, QuickDashboardWidgetProvider::class.java)
    }

    @Test
    fun updateWidget_handlesDualWidgetActiveRecordingState() {
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.VIDEO,
                durationSeconds = 25L
            )
        )
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadow = Shadows.shadowOf(appWidgetManager)

        val dualId = shadow.createWidget(DualRecordWidgetProvider::class.java, R.layout.widget_dual_horizontal_2x1)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dualId, WidgetRecordMode.DUAL, DualRecordWidgetProvider::class.java)

        // Switch to Audio recording and test 2x2 dashboard layout
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 45L
            )
        )
        val dualDashId = shadow.createWidget(DualRecordWidgetProvider::class.java, R.layout.widget_dual_dashboard_2x2)
        val dashOptions = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
        }
        appWidgetManager.updateAppWidgetOptions(dualDashId, dashOptions)
        BaseRecordWidgetProvider.updateWidget(context, appWidgetManager, dualDashId, WidgetRecordMode.DUAL, DualRecordWidgetProvider::class.java)
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

        val dualIntent = Intent(BaseRecordWidgetProvider.ACTION_TOGGLE_RECORD)
        dualProvider.onReceive(context, dualIntent)
    }

    @Test
    fun onReceive_dualWidget_handlesVideoAudioAndStopIntentsGracefully() {
        val videoIntent = Intent(BaseRecordWidgetProvider.ACTION_RECORD_VIDEO)
        dualProvider.onReceive(context, videoIntent)

        val audioIntent = Intent(BaseRecordWidgetProvider.ACTION_RECORD_AUDIO)
        dualProvider.onReceive(context, audioIntent)

        val stopIntent = Intent(BaseRecordWidgetProvider.ACTION_STOP_RECORD)
        dualProvider.onReceive(context, stopIntent)
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

    @Test
    fun onReceive_dualWidgetWhenRecording_triggersStopForAnyAction() {
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.VIDEO,
                durationSeconds = 8L
            )
        )
        // Any tap while recording should safely trigger stop and save
        val videoIntent = Intent(BaseRecordWidgetProvider.ACTION_RECORD_VIDEO)
        dualProvider.onReceive(context, videoIntent)

        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 15L
            )
        )
        val audioIntent = Intent(BaseRecordWidgetProvider.ACTION_RECORD_AUDIO)
        dualProvider.onReceive(context, audioIntent)
    }

    @Test
    fun onReceive_refreshWidgets_updatesAllWidgets() {
        val refreshIntent = Intent(BaseRecordWidgetProvider.ACTION_REFRESH_WIDGETS)
        dualProvider.onReceive(context, refreshIntent)
    }
}

package com.example.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.storage.AppDatabase
import com.example.storage.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_ID = "black_screen_recorder_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_VIDEO = "com.example.recording.ACTION_START_VIDEO"
        const val ACTION_START_AUDIO = "com.example.recording.ACTION_START_AUDIO"
        const val ACTION_STOP = "com.example.recording.ACTION_STOP"
        const val ACTION_VIDEO_SAVED = "com.example.recording.ACTION_VIDEO_SAVED"

        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_PREFERRED_MIC = "extra_preferred_mic"

        private val _recordingStatus = MutableStateFlow(RecordingStatus())
        val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

        private val _stopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val stopEvent: SharedFlow<Unit> = _stopEvent.asSharedFlow()

        private val _liveAudioLevel = MutableStateFlow(0f)
        val liveAudioLevel: StateFlow<Float> = _liveAudioLevel.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var watchdogJob: Job? = null

    private var audioRecorder: AudioRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentRecordingType = RecordingType.VIDEO
    private var recordingStartTime = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BlackScreenRecorder::RecordingLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VIDEO -> {
                currentRecordingType = RecordingType.VIDEO
                startVideoRecordingForeground()
            }
            ACTION_START_AUDIO -> {
                currentRecordingType = RecordingType.AUDIO
                startAudioRecordingForeground()
            }
            ACTION_STOP -> {
                stopActiveRecording()
            }
            ACTION_VIDEO_SAVED -> {
                val path = intent.getStringExtra(EXTRA_FILE_PATH)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (path != null) {
                    saveCompletedVideo(path, duration)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startAudioRecordingForeground() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.ERROR,
                isRecording = false,
                error = getString(R.string.error_mic_unavailable)
            )
            stopSelf()
            return
        }

        wakeLock?.acquire(3 * 60 * 60 * 1000L)
        val notification = createNotification("Audio Recording · Screen kept dark")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val recorder = AudioRecorder(applicationContext)
        audioRecorder = recorder
        val outputFile = recorder.start()

        if (outputFile != null) {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 0L
            )
            startTimer()
            startAmplitudeMonitoring()
        } else {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.ERROR,
                isRecording = false,
                error = "Failed to start microphone recording"
            )
            cleanupAndStop()
        }
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive && _recordingStatus.value.isRecording) {
                delay(100)
                _liveAudioLevel.value = 0.5f
            }
        }
    }

    private fun startVideoRecordingForeground() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.ERROR,
                isRecording = false,
                error = getString(R.string.error_camera_unavailable)
            )
            stopSelf()
            return
        }

        wakeLock?.acquire(3 * 60 * 60 * 1000L)
        val notification = createNotification("Video Recording · Screen kept dark")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (hasMicPermission) {
                fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var fgsType = 0
            if (hasMicPermission) {
                fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            if (fgsType != 0) {
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _recordingStatus.value = RecordingStatus(
            stage = RecordingStage.RECORDING,
            isRecording = true,
            type = RecordingType.VIDEO,
            durationSeconds = 0L
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        recordingStartTime = System.currentTimeMillis()
        timerJob = serviceScope.launch {
            while (isActive && _recordingStatus.value.isRecording) {
                delay(1000)
                val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
                _recordingStatus.value = _recordingStatus.value.copy(durationSeconds = duration)
            }
        }
    }

    private fun stopActiveRecording() {
        timerJob?.cancel()
        _stopEvent.tryEmit(Unit)

        if (currentRecordingType == RecordingType.AUDIO) {
            val file = audioRecorder?.stop()
            val duration = _recordingStatus.value.durationSeconds
            if (file != null && file.exists() && file.length() > 0) {
                serviceScope.launch {
                    try {
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(file.absolutePath),
                            arrayOf("audio/mp4"),
                            null
                        )
                    } catch (_: Exception) {}

                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = RecordingRepository(db.recordingDao(), applicationContext)
                    val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                    repo.insertRecording(
                        title = "Audio $title",
                        file = file,
                        durationSeconds = maxOf(1L, duration),
                        type = "AUDIO"
                    )

                    _recordingStatus.value = RecordingStatus(
                        stage = RecordingStage.STOPPED,
                        isRecording = false,
                        type = RecordingType.AUDIO,
                        durationSeconds = duration,
                        lastSavedFilePath = file.absolutePath,
                        lastSavedTitle = "Audio $title",
                        lastSavedDurationSeconds = duration
                    )
                    cleanupAndStop()
                }
            } else {
                cleanupAndStop()
            }
        }
    }

    private fun saveCompletedVideo(path: String, duration: Long) {
        watchdogJob?.cancel()
        val file = File(path)
        if (file.exists() && file.length() > 0) {
            serviceScope.launch {
                try {
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(file.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                } catch (_: Exception) {}

                val db = AppDatabase.getInstance(applicationContext)
                val repo = RecordingRepository(db.recordingDao(), applicationContext)
                val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                repo.insertRecording(
                    title = "Video $title",
                    file = file,
                    durationSeconds = maxOf(1L, duration),
                    type = "VIDEO"
                )

                _recordingStatus.value = RecordingStatus(
                    stage = RecordingStage.STOPPED,
                    isRecording = false,
                    type = RecordingType.VIDEO,
                    durationSeconds = duration,
                    lastSavedFilePath = file.absolutePath,
                    lastSavedTitle = "Video $title",
                    lastSavedDurationSeconds = duration
                )
                cleanupAndStop()
            }
        } else {
            cleanupAndStop()
        }
    }

    private fun cleanupAndStop() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        _liveAudioLevel.value = 0f
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Black Screen Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing recording status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(subText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Black Screen Recorder")
            .setContentText(subText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(0, "STOP RECORDING", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        timerJob?.cancel()
        audioRecorder?.release()
        audioRecorder = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

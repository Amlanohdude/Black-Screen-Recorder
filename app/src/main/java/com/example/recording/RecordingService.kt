package com.example.recording

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.MainActivity
import com.example.R
import com.example.storage.AppDatabase
import com.example.storage.EntitlementManager
import com.example.storage.PrivateAppStorageProvider
import com.example.storage.RecordingRepository
import com.example.storage.SettingsPreferences
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

class RecordingService : LifecycleService() {

    companion object {
        private const val TAG = "RecordingService"
        const val CHANNEL_ID = "black_screen_recorder_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_VIDEO = "com.example.recording.ACTION_START_VIDEO"
        const val ACTION_START_AUDIO = "com.example.recording.ACTION_START_AUDIO"
        const val ACTION_STOP = "com.example.recording.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.recording.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.recording.ACTION_RESUME"
        const val ACTION_VIDEO_SAVED = "com.example.recording.ACTION_VIDEO_SAVED"

        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_PREFERRED_MIC = "extra_preferred_mic"

        var activeVideoManager: VideoRecordingManager? = null
            private set

        private val _recordingStatus = MutableStateFlow(RecordingStatus())
        val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()

        private val _stopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val stopEvent: SharedFlow<Unit> = _stopEvent.asSharedFlow()

        private val _liveAudioLevel = MutableStateFlow(0f)
        val liveAudioLevel: StateFlow<Float> = _liveAudioLevel.asStateFlow()

        fun updateStatus(status: RecordingStatus) {
            _recordingStatus.value = status
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var watchdogJob: Job? = null

    private var audioRecorder: AudioRecorder? = null
    private var videoManager: VideoRecordingManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var recordingStartTime = 0L
    private var pausedDurationOffsetSeconds = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BlackScreenRecorder::RecordingLock")?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_VIDEO -> {
                startVideoRecordingForeground()
            }
            ACTION_START_AUDIO -> {
                startAudioRecordingForeground()
            }
            ACTION_STOP -> {
                stopActiveRecording()
            }
            ACTION_PAUSE -> {
                pauseActiveRecording()
            }
            ACTION_RESUME -> {
                resumeActiveRecording()
            }
            ACTION_VIDEO_SAVED -> {
                val path = intent.getStringExtra(EXTRA_FILE_PATH)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (path != null) {
                    saveCompletedVideo(path, duration)
                } else {
                    cleanupAndStop()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val prefs = SettingsPreferences(applicationContext).settings.value
        val audioConfig = AudioRecordingConfig(
            microphone = AudioMicSource.fromId(prefs.preferredMicrophone),
            quality = AudioQuality.fromId(prefs.audioQuality),
            clearVoice = prefs.audioClearVoice,
            reduceEcho = prefs.audioReduceEcho,
            inputVolume = prefs.audioInputVolume,
            channels = prefs.audioChannels,
            bitrate = prefs.audioBitrate,
            sampleRate = prefs.audioSampleRate
        )

        val recorder = AudioRecorder(applicationContext, PrivateAppStorageProvider(applicationContext))
        audioRecorder = recorder
        val outputFile = recorder.start(audioConfig)

        if (outputFile != null) {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.RECORDING,
                isRecording = true,
                type = RecordingType.AUDIO,
                durationSeconds = 0L
            )
            val settings = SettingsPreferences(applicationContext).settings.value
            if (settings.hapticFeedbackEnabled) {
                BlindOperationHaptics.vibrateStarted(applicationContext)
            }
            startTimer()
            startAmplitudeMonitoring()
        } else {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.ERROR,
                isRecording = false,
                error = getString(R.string.error_mic_unavailable)
            )
            cleanupAndStop()
        }
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive && _recordingStatus.value.isRecording) {
                delay(150L)
                val amp = audioRecorder?.getMaxAmplitude() ?: 0
                val normalized = if (amp > 0) {
                    ((Math.sqrt(amp / 32767.0)).toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                _liveAudioLevel.value = normalized
            }
        }
    }

    private fun startVideoRecordingForeground() {
        if (_recordingStatus.value.isRecording) {
            return
        }

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

        val prefs = SettingsPreferences(applicationContext).settings.value
        val manager = VideoRecordingManager(applicationContext, PrivateAppStorageProvider(applicationContext))
        this.videoManager = manager
        activeVideoManager = manager

        manager.initializeCamera(
            lifecycleOwner = this,
            previewView = null,
            useFrontCamera = prefs.useFrontCamera,
            qualityStr = prefs.videoQuality,
            onReady = {
                val file = manager.startRecording(
                    hasAudioPermission = hasMicPermission,
                    preferredMic = AudioMicSource.fromId(prefs.preferredMicrophone),
                    onFinalized = { savedFile, success ->
                        handleVideoFinalized(savedFile, success)
                    }
                )
                if (file != null) {
                    _recordingStatus.value = RecordingStatus(
                        stage = RecordingStage.RECORDING,
                        isRecording = true,
                        type = RecordingType.VIDEO,
                        durationSeconds = 0L
                    )
                    val settings = SettingsPreferences(applicationContext).settings.value
                    if (settings.hapticFeedbackEnabled) {
                        BlindOperationHaptics.vibrateStarted(applicationContext)
                    }
                    startTimer()
                } else {
                    _recordingStatus.value = RecordingStatus(
                        stage = RecordingStage.ERROR,
                        isRecording = false,
                        error = getString(R.string.error_camera_unavailable)
                    )
                    cleanupAndStop()
                }
            },
            onError = { errorMsg ->
                _recordingStatus.value = RecordingStatus(
                    stage = RecordingStage.ERROR,
                    isRecording = false,
                    error = errorMsg
                )
                cleanupAndStop()
            }
        )
    }

    private fun startTimer(initialOffsetSeconds: Long = 0L) {
        timerJob?.cancel()
        pausedDurationOffsetSeconds = initialOffsetSeconds
        recordingStartTime = System.currentTimeMillis()
        timerJob = serviceScope.launch {
            while (isActive && _recordingStatus.value.isRecording && _recordingStatus.value.stage == RecordingStage.RECORDING) {
                delay(1000)
                val duration = pausedDurationOffsetSeconds + (System.currentTimeMillis() - recordingStartTime) / 1000
                _recordingStatus.value = _recordingStatus.value.copy(durationSeconds = duration)

                val prefs = SettingsPreferences(applicationContext).settings.value
                val maxSeconds = prefs.autoStopSeconds
                if (maxSeconds > 0 && EntitlementManager.canUseAutoStop() && duration >= maxSeconds) {
                    stopActiveRecording()
                    break
                }
            }
        }
    }

    private fun pauseActiveRecording() {
        if (_recordingStatus.value.stage != RecordingStage.RECORDING) return
        val currentDuration = _recordingStatus.value.durationSeconds
        pausedDurationOffsetSeconds = currentDuration
        timerJob?.cancel()
        amplitudeJob?.cancel()

        val type = _recordingStatus.value.type
        if (type == RecordingType.VIDEO) {
            videoManager?.pauseRecording()
        } else if (type == RecordingType.AUDIO) {
            audioRecorder?.pause()
        }

        _recordingStatus.value = _recordingStatus.value.copy(
            stage = RecordingStage.PAUSED
        )

        val settings = SettingsPreferences(applicationContext).settings.value
        if (settings.hapticFeedbackEnabled) {
            BlindOperationHaptics.vibratePaused(applicationContext)
        }
    }

    private fun resumeActiveRecording() {
        if (_recordingStatus.value.stage != RecordingStage.PAUSED) return
        val type = _recordingStatus.value.type
        if (type == RecordingType.VIDEO) {
            videoManager?.resumeRecording()
        } else if (type == RecordingType.AUDIO) {
            audioRecorder?.resume()
            startAmplitudeMonitoring()
        }

        _recordingStatus.value = _recordingStatus.value.copy(
            stage = RecordingStage.RECORDING
        )

        val settings = SettingsPreferences(applicationContext).settings.value
        if (settings.hapticFeedbackEnabled) {
            BlindOperationHaptics.vibrateStarted(applicationContext)
        }

        startTimer(pausedDurationOffsetSeconds)
    }

    private fun stopActiveRecording() {
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
        _liveAudioLevel.value = 0f
        _stopEvent.tryEmit(Unit)

        val current = _recordingStatus.value
        val duration = current.durationSeconds
        val type = current.type

        if (type == RecordingType.AUDIO) {
            _recordingStatus.value = current.copy(
                stage = RecordingStage.FINALIZING,
                isRecording = false
            )
            val audioFile = audioRecorder?.stop()
            audioRecorder?.release()
            audioRecorder = null

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                serviceScope.launch(Dispatchers.IO) {
                    val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                    try {
                        val db = AppDatabase.getInstance(applicationContext)
                        val repo = RecordingRepository(db.recordingDao(), applicationContext)
                        repo.insertRecording(
                            title = "Audio $title",
                            file = audioFile,
                            durationSeconds = maxOf(1L, duration),
                            type = "AUDIO"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving audio to db", e)
                    }

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        val settings = SettingsPreferences(applicationContext).settings.value
                        if (settings.hapticFeedbackEnabled) {
                            BlindOperationHaptics.vibrateSaved(applicationContext)
                        }
                        _recordingStatus.value = RecordingStatus(
                            stage = RecordingStage.IDLE,
                            isRecording = false,
                            type = null,
                            durationSeconds = 0L,
                            lastSavedFilePath = audioFile.absolutePath,
                            lastSavedTitle = "Audio $title",
                            lastSavedDurationSeconds = duration,
                            lastSavedSizeBytes = audioFile.length()
                        )
                        cleanupAndStop()
                    }
                }
            } else {
                _recordingStatus.value = RecordingStatus(
                    stage = RecordingStage.IDLE,
                    isRecording = false
                )
                cleanupAndStop()
            }
            return
        }

        if (type != RecordingType.VIDEO) {
            cleanupAndStop()
            return
        }

        _recordingStatus.value = current.copy(
            stage = RecordingStage.FINALIZING,
            isRecording = false
        )
        videoManager?.stopRecording()

        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            delay(10000L)
            cleanupAndStop()
        }
    }

    private fun handleVideoFinalized(file: File?, success: Boolean) {
        watchdogJob?.cancel()
        watchdogJob = null
        val duration = maxOf(1L, _recordingStatus.value.durationSeconds)

        if (success && file != null && file.exists() && file.length() > 0) {
            serviceScope.launch(Dispatchers.IO) {
                val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = RecordingRepository(db.recordingDao(), applicationContext)
                    repo.insertRecording(
                        title = "Video $title",
                        file = file,
                        durationSeconds = duration,
                        type = "VIDEO"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving video to db", e)
                }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val settings = SettingsPreferences(applicationContext).settings.value
                    if (settings.hapticFeedbackEnabled) {
                        BlindOperationHaptics.vibrateSaved(applicationContext)
                    }
                    _recordingStatus.value = RecordingStatus(
                        stage = RecordingStage.IDLE,
                        isRecording = false,
                        type = null,
                        durationSeconds = 0L,
                        lastSavedFilePath = file.absolutePath,
                        lastSavedTitle = "Video $title",
                        lastSavedDurationSeconds = duration,
                        lastSavedSizeBytes = file.length()
                    )
                    cleanupAndStop()
                }
            }
            return
        }

        if (file != null && file.exists() && file.length() == 0L) {
            file.delete()
        }
        _recordingStatus.value = RecordingStatus(
            stage = RecordingStage.ERROR,
            isRecording = false,
            error = "Video recording could not be saved safely."
        )
        cleanupAndStop()
    }

    private fun saveCompletedVideo(path: String, duration: Long) {
        watchdogJob?.cancel()
        watchdogJob = null
        val file = File(path)
        if (file.exists() && file.length() > 0) {
            serviceScope.launch(Dispatchers.IO) {
                val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = RecordingRepository(db.recordingDao(), applicationContext)
                    repo.insertRecording(
                        title = "Video $title",
                        file = file,
                        durationSeconds = maxOf(1L, duration),
                        type = "VIDEO"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving completed video to db", e)
                }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val settings = SettingsPreferences(applicationContext).settings.value
                    if (settings.hapticFeedbackEnabled) {
                        BlindOperationHaptics.vibrateSaved(applicationContext)
                    }
                    _recordingStatus.value = RecordingStatus(
                        stage = RecordingStage.IDLE,
                        isRecording = false,
                        type = null,
                        durationSeconds = 0L,
                        lastSavedFilePath = file.absolutePath,
                        lastSavedTitle = "Video $title",
                        lastSavedDurationSeconds = duration,
                        lastSavedSizeBytes = file.length()
                    )
                    cleanupAndStop()
                }
            }
        } else {
            _recordingStatus.value = RecordingStatus(
                stage = RecordingStage.IDLE,
                isRecording = false
            )
            cleanupAndStop()
        }
    }

    private fun cleanupAndStop() {
        watchdogJob?.cancel()
        watchdogJob = null
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
        _liveAudioLevel.value = 0f

        videoManager?.release()
        videoManager = null
        activeVideoManager = null

        audioRecorder?.release()
        audioRecorder = null

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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val prefs = SettingsPreferences(applicationContext).settings.value
        // If background recording is turned off, swiping away the task immediately stops and saves recording cleanly
        if (!prefs.backgroundRecordingEnabled && _recordingStatus.value.isRecording) {
            stopActiveRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        watchdogJob = null
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null

        // Emergency flush: if service is killed unexpectedly by OS while recording, finalize and flush
        if (_recordingStatus.value.isRecording) {
            try {
                videoManager?.stopRecording()
                val audioFile = audioRecorder?.stop()
                if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                    kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                        try {
                            val db = AppDatabase.getInstance(applicationContext)
                            val title = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                            db.recordingDao().insertRecording(
                                com.example.storage.RecordingEntity(
                                    title = "Audio $title",
                                    filePath = audioFile.absolutePath,
                                    durationSeconds = maxOf(1L, _recordingStatus.value.durationSeconds),
                                    fileSizeBytes = audioFile.length(),
                                    timestamp = System.currentTimeMillis(),
                                    type = "AUDIO"
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        videoManager?.release()
        videoManager = null
        activeVideoManager = null
        audioRecorder?.release()
        audioRecorder = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

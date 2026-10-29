package com.example.recording

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recorder
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.storage.PrivateAppStorageProvider
import com.example.storage.RecordingStorageProvider
import java.io.File

class VideoRecordingManager(
    private val context: Context,
    private val storageProvider: RecordingStorageProvider = PrivateAppStorageProvider(context)
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var activeRecording: Recording? = null
    private var currentVideoFile: File? = null
    private var routingSession: AudioRoutingSession? = null

    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        useFrontCamera: Boolean,
        qualityStr: String,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val quality = when (qualityStr) {
                    "720p" -> Quality.HD
                    "480p" -> Quality.SD
                    else -> Quality.FHD
                }

                val qualitySelector = QualitySelector.from(
                    quality,
                    androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .setExecutor(ContextCompat.getMainExecutor(context))
                    .build()

                val capture = VideoCapture.withOutput(recorder)
                videoCapture = capture

                val previewUseCase = Preview.Builder().build()
                this.preview = previewUseCase
                if (previewView != null) {
                    previewUseCase.surfaceProvider = previewView.surfaceProvider
                }

                provider.bindToLifecycle(lifecycleOwner, cameraSelector, capture, previewUseCase)

                onReady()
            } catch (e: Exception) {
                Log.e("VideoRecordingManager", "Error initializing camera", e)
                onError(e.message ?: "Camera unavailable")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording(
        hasAudioPermission: Boolean,
        preferredMic: AudioMicSource = AudioMicSource.PHONE_MIC,
        onFinalized: (File?, Boolean) -> Unit
    ): File? {
        val capture = videoCapture ?: return null

        val file = storageProvider.createVideoOutputFile()
        currentVideoFile = file

        val outputOptions = FileOutputOptions.Builder(file).build()
        val pendingRecording = capture.output.prepareRecording(context, outputOptions)

        if (hasAudioPermission) {
            try {
                // Route to preferred microphone device for video audio capture
                routingSession = AudioDeviceRouter.routeAudioInput(context, preferredMic, null)
                pendingRecording.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w("VideoRecordingManager", "Audio recording permission missing", e)
            }
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    Log.d("VideoRecordingManager", "Video recording started successfully in private storage")
                }
                is VideoRecordEvent.Finalize -> {
                    routingSession?.release()
                    routingSession = null

                    val hasData = file.exists() && file.length() > 0
                    val success = (!event.hasError() || hasData) && hasData
                    if (event.hasError()) {
                        Log.e("VideoRecordingManager", "Video recording event finalize error: ${event.error}")
                    }

                    if (success) {
                        // Note: Intentionally do NOT scan with MediaScanner to maintain privacy
                        onFinalized(file, true)
                    } else {
                        // Clean up corrupted or zero-byte file
                        if (file.exists() && file.length() == 0L) {
                            file.delete()
                        }
                        onFinalized(null, false)
                    }
                    activeRecording = null
                    currentVideoFile = null
                }
            }
        }

        return file
    }

    fun setPreviewSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        try {
            preview?.surfaceProvider = surfaceProvider
        } catch (e: Exception) {
            Log.e("VideoRecordingManager", "Error setting preview surface", e)
        }
    }

    fun pauseRecording() {
        try {
            activeRecording?.pause()
        } catch (e: Exception) {
            Log.e("VideoRecordingManager", "Error pausing video recording", e)
        }
    }

    fun resumeRecording() {
        try {
            activeRecording?.resume()
        } catch (e: Exception) {
            Log.e("VideoRecordingManager", "Error resuming video recording", e)
        }
    }

    fun stopRecording() {
        try {
            routingSession?.release()
            routingSession = null
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.e("VideoRecordingManager", "Error stopping recording", e)
        }
    }

    fun release() {
        stopRecording()
        try {
            routingSession?.release()
            routingSession = null
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        cameraProvider = null
        videoCapture = null
        preview = null
        activeRecording = null
        currentVideoFile = null
    }
}

package com.example.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.storage.PrivateAppStorageProvider
import com.example.storage.RecordingStorageProvider
import java.io.File

class AudioRecorder(
    private val context: Context,
    private val storageProvider: RecordingStorageProvider = PrivateAppStorageProvider(context)
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var routingSession: AudioRoutingSession? = null
    private var isRecording = false

    fun start(config: AudioRecordingConfig = AudioRecordingConfig()): File? {
        if (isRecording) {
            return currentOutputFile
        }

        return try {
            val file = storageProvider.createAudioOutputFile()
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            val audioSource = if (config.clearVoice) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else if (config.reduceEcho) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            }

            recorder.setAudioSource(audioSource)
            routingSession = AudioDeviceRouter.routeAudioInput(context, config.microphone, recorder)

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(config.bitrate)
            recorder.setAudioSamplingRate(config.sampleRate)
            try {
                recorder.setAudioChannels(config.channels)
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Device does not support ${config.channels} channels, falling back to mono", e)
                recorder.setAudioChannels(1)
            }
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecording = true
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start audio recording", e)
            release()
            currentOutputFile?.let { if (it.exists() && it.length() == 0L) it.delete() }
            currentOutputFile = null
            null
        }
    }

    fun getMaxAmplitude(): Int {
        if (!isRecording) return 0
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun stop(): File? {
        if (!isRecording) return currentOutputFile
        val file = currentOutputFile
        return try {
            routingSession?.release()
            routingSession = null

            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentOutputFile = null

            // Validate recording integrity
            if (file != null && file.exists() && file.length() > 0) {
                file
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping audio recorder", e)
            release()
            if (file != null && file.exists() && file.length() > 0) {
                file
            } else {
                file?.delete()
                null
            }
        }
    }

    fun release() {
        try {
            routingSession?.release()
            routingSession = null
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
    }

    fun isRecording(): Boolean = isRecording
}

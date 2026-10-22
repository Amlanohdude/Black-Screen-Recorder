package com.example.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun start(): File? {
        if (isRecording) {
            return currentOutputFile
        }

        return try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "recordings_audio")
            val audioDir = File(baseDir, "BlackScreenRecorder").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(audioDir, "AUD_${timeStamp}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

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

    fun stop(): File? {
        if (!isRecording) return currentOutputFile
        val file = currentOutputFile
        return try {
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
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
    }

    fun isRecording(): Boolean = isRecording
}

package com.example.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val context: Context
) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun insertRecording(
        title: String,
        file: File,
        durationSeconds: Long,
        type: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = RecordingEntity(
            title = title,
            filePath = file.absolutePath,
            durationSeconds = durationSeconds,
            fileSizeBytes = if (file.exists()) file.length() else 0L,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        recordingDao.insertRecording(entity)
    }

    suspend fun updateTitle(recording: RecordingEntity, newTitle: String) = withContext(Dispatchers.IO) {
        val updated = recording.copy(title = newTitle.trim())
        recordingDao.updateRecording(updated)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteRecordings(recordings: List<RecordingEntity>) = withContext(Dispatchers.IO) {
        recordings.forEach { recording ->
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
            recordingDao.deleteRecording(recording)
        }
    }

    suspend fun purgeRecordingsOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        if (days <= 0) return@withContext 0
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
        val expired = recordingDao.getRecordingsOlderThan(cutoff)
        expired.forEach { recording ->
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
            recordingDao.deleteRecording(recording)
        }
        expired.size
    }

    suspend fun exportRecordingToCustomFolder(
        recording: RecordingEntity,
        folderUriString: String?,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult {
        return RecordingExportManager.exportRecording(context, recording, folderUriString, onProgress, isCancelled)
    }

    suspend fun exportBatchToCustomFolder(
        recordings: List<RecordingEntity>,
        folderUriString: String?,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult {
        return RecordingExportManager.exportBatchRecordings(context, recordings, folderUriString, onProgress, isCancelled)
    }

    fun getShareIntent(recording: RecordingEntity): Intent? {
        val file = File(recording.filePath)
        if (!file.exists()) return null

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            return null
        }

        val mimeType = if (recording.type == "VIDEO") "video/mp4" else "audio/mp4"
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

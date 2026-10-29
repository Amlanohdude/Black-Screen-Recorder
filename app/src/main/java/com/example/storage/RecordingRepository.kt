package com.example.storage

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun getRecordingById(id: Long): RecordingEntity? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingById(id)
    }

    suspend fun getRecordingByFilePath(filePath: String): RecordingEntity? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingByFilePath(filePath)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    try {
                        java.io.FileOutputStream(file).close()
                    } catch (_: Exception) {}
                    file.delete()
                }
            }
        } catch (_: Exception) {
        }
        recordingDao.deleteRecording(recording)
        recordingDao.deleteRecordingByFilePath(recording.filePath)
    }

    suspend fun deleteRecordingByFilePath(filePath: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    try {
                        java.io.FileOutputStream(file).close()
                    } catch (_: Exception) {}
                    file.delete()
                }
            }
        } catch (_: Exception) {
        }
        recordingDao.deleteRecordingByFilePath(filePath)
    }

    suspend fun deleteRecordings(recordings: List<RecordingEntity>) = withContext(Dispatchers.IO) {
        recordings.forEach { recording ->
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (!deleted) {
                        try {
                            java.io.FileOutputStream(file).close()
                        } catch (_: Exception) {}
                        file.delete()
                    }
                }
            } catch (_: Exception) {
            }
            recordingDao.deleteRecording(recording)
            recordingDao.deleteRecordingByFilePath(recording.filePath)
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
                    val deleted = file.delete()
                    if (!deleted) {
                        try {
                            java.io.FileOutputStream(file).close()
                        } catch (_: Exception) {}
                        file.delete()
                    }
                }
            } catch (_: Exception) {
            }
            recordingDao.deleteRecording(recording)
            recordingDao.deleteRecordingByFilePath(recording.filePath)
        }
        expired.size
    }

    /**
     * Automatic reconciliation & crash recovery:
     * Scans the internal recordings directory to discover any audio/video files that exist on disk
     * but are not registered in the Room database (e.g. if the process was terminated, device restarted,
     * or battery died before DB insertion). Safely indexes them so recordings NEVER vanish.
     */
    suspend fun reconcileOrphanFiles(): Int = withContext(Dispatchers.IO) {
        var recoveredCount = 0
        try {
            val dir = File(context.filesDir, "recordings")
            if (!dir.exists() || !dir.isDirectory) return@withContext 0
            val files = dir.listFiles() ?: return@withContext 0
            for (file in files) {
                if (!file.isFile) continue
                if (file.length() == 0L) {
                    // Safe cleanup of abandoned 0-byte corrupt files
                    file.delete()
                    continue
                }
                val isMp4 = file.name.endsWith(".mp4", ignoreCase = true)
                val isM4a = file.name.endsWith(".m4a", ignoreCase = true)
                if (!isMp4 && !isM4a) continue

                val count = recordingDao.hasRecordingWithPath(file.absolutePath)
                if (count == 0) {
                    val durationSeconds = extractDurationSeconds(file)
                    val isVideo = isMp4
                    val type = if (isVideo) "VIDEO" else "AUDIO"
                    val dateFormatted = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                    val title = (if (isVideo) "Video " else "Audio ") + dateFormatted
                    val entity = RecordingEntity(
                        title = title,
                        filePath = file.absolutePath,
                        durationSeconds = maxOf(1L, durationSeconds),
                        fileSizeBytes = file.length(),
                        timestamp = file.lastModified(),
                        type = type
                    )
                    recordingDao.insertRecording(entity)
                    recoveredCount++
                    Log.i("RecordingRepository", "Successfully recovered orphan recording: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("RecordingRepository", "Error reconciling orphan files", e)
        }
        recoveredCount
    }

    private fun extractDurationSeconds(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durMs = durStr?.toLongOrNull() ?: 0L
            maxOf(1L, durMs / 1000L)
        } catch (_: Exception) {
            1L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
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

    fun getBatchShareIntent(recordings: List<RecordingEntity>): Intent? {
        if (recordings.isEmpty()) return null
        val uris = ArrayList<Uri>()
        var hasVideo = false
        var hasAudio = false

        for (rec in recordings) {
            val file = File(rec.filePath)
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    uris.add(uri)
                    if (rec.type.equals("VIDEO", ignoreCase = true)) hasVideo = true else hasAudio = true
                } catch (e: Exception) {
                    Log.w("RecordingRepository", "Could not get Uri for file: ${rec.filePath}", e)
                }
            }
        }

        if (uris.isEmpty()) return null

        val mimeType = when {
            hasVideo && !hasAudio -> "video/*"
            !hasVideo && hasAudio -> "audio/*"
            else -> "*/*"
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share ${uris.size} recordings")
    }
}

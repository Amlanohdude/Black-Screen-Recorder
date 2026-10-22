package com.example.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Handles explicit user-requested exports from private app storage to shared device storage or SAF URIs.
 *
 * Exporting creates a public copy in shared storage while keeping the private original intact.
 */
object ExportManager {

    /**
     * Exports a private recording to public MediaStore (Movies or Music).
     * Returns the content Uri of the exported file upon success.
     */
    suspend fun exportToSharedStorage(
        context: Context,
        recording: RecordingEntity
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val sourceFile = File(recording.filePath)
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext Result.failure(IllegalStateException("Source recording file not found"))
        }

        val isVideo = recording.type.equals("VIDEO", ignoreCase = true)
        val mimeType = if (isVideo) "video/mp4" else "audio/mp4"
        val extension = if (isVideo) ".mp4" else ".m4a"
        val filename = "BSR_EXPORT_${System.currentTimeMillis()}$extension"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val relativePath = if (isVideo) {
                    "${Environment.DIRECTORY_MOVIES}/BlackScreenRecorder"
                } else {
                    "${Environment.DIRECTORY_MUSIC}/BlackScreenRecorder"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val destinationUri = resolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry"))

                try {
                    resolver.openOutputStream(destinationUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext Result.failure(IllegalStateException("Could not open destination output stream"))

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destinationUri, contentValues, null, null)

                    Result.success(destinationUri)
                } catch (e: Exception) {
                    resolver.delete(destinationUri, null, null)
                    Result.failure(e)
                }
            } else {
                // Legacy Android fallback
                @Suppress("DEPRECATION")
                val publicDir = if (isVideo) {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                } else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                }
                val targetDir = File(publicDir, "BlackScreenRecorder").apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, filename)

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                Result.success(Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a private recording to a user-selected SAF document Uri.
     */
    suspend fun exportToDocumentUri(
        context: Context,
        sourceFilePath: String,
        destinationUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext Result.failure(IllegalStateException("Source file not found"))
        }

        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream for selected folder"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

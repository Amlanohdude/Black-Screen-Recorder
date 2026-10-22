package com.example.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream

data class ExportProgress(
    val currentFileIndex: Int = 1,
    val totalFiles: Int = 1,
    val bytesCopied: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Int = 0,
    val currentFileName: String = ""
)

sealed class ExportResult {
    data class Success(
        val exportedCount: Int,
        val destinationFolderName: String,
        val message: String = "Recording exported successfully."
    ) : ExportResult()

    data class Failure(
        val message: String,
        val errorType: ExportErrorType
    ) : ExportResult()

    object Cancelled : ExportResult()
}

enum class ExportErrorType {
    NO_FOLDER_CONFIGURED,
    FOLDER_PERMISSION_REVOKED,
    SOURCE_MISSING,
    INSUFFICIENT_STORAGE,
    IO_ERROR
}

/**
 * Manages explicit exports of private recordings to user-selected Custom Folders via SAF.
 */
object RecordingExportManager {

    private const val BUFFER_SIZE = 32 * 1024 // 32 KB buffer

    fun getFolderDisplayName(context: Context, treeUri: Uri): String {
        return try {
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            val name = docFile?.name
            if (!name.isNullOrBlank()) {
                name
            } else {
                val seg = treeUri.lastPathSegment ?: ""
                if (seg.contains(":")) seg.substringAfterLast(":") else seg.ifBlank { "Custom Folder" }
            }
        } catch (_: Exception) {
            "Custom Folder"
        }
    }

    suspend fun exportRecording(
        context: Context,
        recording: RecordingEntity,
        folderUriString: String?,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult = withContext(Dispatchers.IO) {
        if (folderUriString.isNullOrBlank()) {
            return@withContext ExportResult.Failure("No custom folder selected.", ExportErrorType.NO_FOLDER_CONFIGURED)
        }
        try {
            val treeUri = Uri.parse(folderUriString)
            val destinationTree = try {
                DocumentFile.fromTreeUri(context, treeUri)
            } catch (_: Exception) {
                null
            }

            if (destinationTree == null || !destinationTree.exists() || !destinationTree.canWrite()) {
                return@withContext ExportResult.Failure("The selected custom folder is no longer available.", ExportErrorType.FOLDER_PERMISSION_REVOKED)
            }

            val sourceFile = File(recording.filePath)
            if (!sourceFile.exists() || !sourceFile.canRead() || sourceFile.length() == 0L) {
                return@withContext ExportResult.Failure("This recording is no longer available.", ExportErrorType.SOURCE_MISSING)
            }

            val folderDisplayName = getFolderDisplayName(context, treeUri)
            val copyResult = copyRecordingToTree(context, sourceFile, destinationTree, recording.type, 1, 1, onProgress, isCancelled)

            when (copyResult) {
                is CopyFileResult.Success -> ExportResult.Success(1, folderDisplayName, "Recording exported to $folderDisplayName.")
                is CopyFileResult.Cancelled -> ExportResult.Cancelled
                is CopyFileResult.Error -> ExportResult.Failure(copyResult.message, copyResult.errorType)
            }
        } catch (_: Exception) {
            ExportResult.Failure("The selected custom folder is no longer available.", ExportErrorType.FOLDER_PERMISSION_REVOKED)
        }
    }

    suspend fun exportBatchRecordings(
        context: Context,
        recordings: List<RecordingEntity>,
        folderUriString: String?,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult = withContext(Dispatchers.IO) {
        if (recordings.isEmpty()) {
            return@withContext ExportResult.Success(0, "")
        }
        if (folderUriString.isNullOrBlank()) {
            return@withContext ExportResult.Failure("No custom folder selected.", ExportErrorType.NO_FOLDER_CONFIGURED)
        }
        try {
            val treeUri = Uri.parse(folderUriString)
            val destinationTree = try {
                DocumentFile.fromTreeUri(context, treeUri)
            } catch (_: Exception) {
                null
            }

            if (destinationTree == null || !destinationTree.exists() || !destinationTree.canWrite()) {
                return@withContext ExportResult.Failure("The selected custom folder is no longer available.", ExportErrorType.FOLDER_PERMISSION_REVOKED)
            }

            val folderDisplayName = getFolderDisplayName(context, treeUri)
            var successCount = 0
            var failCount = 0

            for ((index, recording) in recordings.withIndex()) {
                if (isCancelled()) {
                    return@withContext ExportResult.Cancelled
                }
                val sourceFile = File(recording.filePath)
                if (!sourceFile.exists() || !sourceFile.canRead() || sourceFile.length() == 0L) {
                    failCount++
                    continue
                }

                val copyResult = copyRecordingToTree(context, sourceFile, destinationTree, recording.type, index + 1, recordings.size, onProgress, isCancelled)
                when (copyResult) {
                    is CopyFileResult.Success -> successCount++
                    is CopyFileResult.Cancelled -> return@withContext ExportResult.Cancelled
                    is CopyFileResult.Error -> failCount++
                }
            }

            if (successCount == recordings.size) {
                ExportResult.Success(successCount, folderDisplayName, "$successCount recordings exported to $folderDisplayName.")
            } else if (successCount > 0) {
                ExportResult.Success(successCount, folderDisplayName, "$successCount exported, $failCount failed. Originals remain safe.")
            } else {
                ExportResult.Failure("Export failed. Your original recordings are still safe.", ExportErrorType.IO_ERROR)
            }
        } catch (_: Exception) {
            ExportResult.Failure("The selected custom folder is no longer available.", ExportErrorType.FOLDER_PERMISSION_REVOKED)
        }
    }

    private sealed class CopyFileResult {
        object Success : CopyFileResult()
        object Cancelled : CopyFileResult()
        data class Error(val message: String, val errorType: ExportErrorType) : CopyFileResult()
    }

    private fun copyRecordingToTree(
        context: Context,
        sourceFile: File,
        destinationTree: DocumentFile,
        recordingType: String,
        fileIndex: Int,
        totalFiles: Int,
        onProgress: (ExportProgress) -> Unit,
        isCancelled: () -> Boolean
    ): CopyFileResult {
        val isVideo = recordingType.equals("VIDEO", ignoreCase = true)
        val mimeType = if (isVideo) "video/mp4" else "audio/mp4"
        val originalName = sourceFile.name
        val safeDestinationName = resolveConflictName(destinationTree, originalName)

        val destinationDoc = destinationTree.createFile(mimeType, safeDestinationName)
            ?: return CopyFileResult.Error(
                message = "Not enough storage space or permission denied to export this recording.",
                errorType = ExportErrorType.INSUFFICIENT_STORAGE
            )

        val totalBytes = sourceFile.length()
        var bytesCopied = 0L

        try {
            val input = BufferedInputStream(FileInputStream(sourceFile), BUFFER_SIZE)
            val outputStream = context.contentResolver.openOutputStream(destinationDoc.uri)
                ?: run {
                    safeDelete(destinationDoc)
                    return CopyFileResult.Error("Cannot write to destination.", ExportErrorType.IO_ERROR)
                }
            val output = BufferedOutputStream(outputStream, BUFFER_SIZE)

            val buffer = ByteArray(BUFFER_SIZE)
            input.use { inStream ->
                output.use { outStream ->
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled()) {
                            safeDelete(destinationDoc)
                            return CopyFileResult.Cancelled
                        }
                        outStream.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                        val pct = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                        onProgress(
                            ExportProgress(
                                currentFileIndex = fileIndex,
                                totalFiles = totalFiles,
                                bytesCopied = bytesCopied,
                                totalBytes = totalBytes,
                                percent = pct,
                                currentFileName = originalName
                            )
                        )
                    }
                    outStream.flush()
                }
            }
            return CopyFileResult.Success
        } catch (e: Exception) {
            safeDelete(destinationDoc)
            return CopyFileResult.Error(e.message ?: "Export failed.", ExportErrorType.IO_ERROR)
        }
    }

    private fun resolveConflictName(treeDoc: DocumentFile, originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var candidate = originalName
        var counter = 1
        while (treeDoc.findFile(candidate) != null && counter < 1000) {
            candidate = "$baseName ($counter)$extension"
            counter++
        }
        return candidate
    }

    fun resolveConflictName(existingNames: Set<String>, originalName: String): String {
        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var candidate = originalName
        var counter = 1
        while (existingNames.contains(candidate) && counter < 1000) {
            candidate = "$baseName ($counter)$extension"
            counter++
        }
        return candidate
    }

    private fun safeDelete(doc: DocumentFile?) {
        try {
            doc?.delete()
        } catch (_: Exception) {
        }
    }
}

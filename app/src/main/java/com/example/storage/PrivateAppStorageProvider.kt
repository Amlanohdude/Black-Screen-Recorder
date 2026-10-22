package com.example.storage

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Default storage provider storing recordings strictly within the app's internal sandbox (filesDir).
 *
 * Files stored here are isolated by the Android OS and not indexed by MediaStore or accessible
 * to other applications without root/backup access.
 */
class PrivateAppStorageProvider(private val context: Context) : RecordingStorageProvider {

    override val storageType: StorageType = StorageType.PRIVATE_APP

    override fun getRecordingsDirectory(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    override fun createAudioOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getRecordingsDirectory(), "AUD_${timeStamp}.m4a")
    }

    override fun createVideoOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getRecordingsDirectory(), "VID_${timeStamp}.mp4")
    }
}

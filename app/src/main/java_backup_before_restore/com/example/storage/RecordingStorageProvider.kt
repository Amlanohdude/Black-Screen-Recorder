package com.example.storage

import java.io.File

interface RecordingStorageProvider {
    val storageType: StorageType
    fun getRecordingsDirectory(): File
    fun createAudioOutputFile(): File
    fun createVideoOutputFile(): File
}

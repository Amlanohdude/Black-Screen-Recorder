package com.example.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "VIDEO" or "AUDIO"
    val storageType: String = StorageType.PRIVATE_APP.name
)

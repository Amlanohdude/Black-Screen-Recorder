package com.example.ui.recorder

import com.example.recording.RecordingType
import com.example.recording.SafetyStopReason

data class RecordingSummaryInfo(
    val type: RecordingType,
    val title: String,
    val filePath: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long = 0L,
    val qualityOrFormat: String = "",
    val coverName: String = "",
    val safetyStopReason: SafetyStopReason? = null
)

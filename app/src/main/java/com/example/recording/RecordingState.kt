package com.example.recording

enum class RecordingStage {
    IDLE,
    STARTING,
    RECORDING,
    FINALIZING,
    ERROR
}

data class RecordingStatus(
    val stage: RecordingStage = RecordingStage.IDLE,
    val isRecording: Boolean = false,
    val type: RecordingType? = null,
    val durationSeconds: Long = 0L,
    val error: String? = null,
    val lastSavedTitle: String? = null,
    val lastSavedFilePath: String? = null,
    val lastSavedDurationSeconds: Long = 0L,
    val lastSavedSizeBytes: Long = 0L
)

package com.example.recording

enum class RecordingStage {
    IDLE,
    RECORDING,
    PAUSED,
    STOPPED,
    ERROR
}

data class RecordingStatus(
    val stage: RecordingStage = RecordingStage.IDLE,
    val isRecording: Boolean = false,
    val type: RecordingType = RecordingType.VIDEO,
    val durationSeconds: Long = 0L,
    val error: String? = null,
    val lastSavedFilePath: String? = null,
    val lastSavedTitle: String? = null,
    val lastSavedDurationSeconds: Long = 0L
)

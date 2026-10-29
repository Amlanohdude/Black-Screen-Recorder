package com.example.recording

enum class RecordingStage {
    IDLE,
    STARTING,
    RECORDING,
    PAUSED,
    FINALIZING,
    ERROR
}

enum class SafetyStopReason(val displayName: String, val description: String) {
    BATTERY_LOW("Low Battery Guardrail", "Recording safely finalized before battery cutoff"),
    STORAGE_LOW("Low Storage Guardrail", "Recording safely finalized before disk overflow")
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
    val lastSavedSizeBytes: Long = 0L,
    val safetyStopReason: SafetyStopReason? = null
)

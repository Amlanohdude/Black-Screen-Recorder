package com.example.storage

/**
 * Represents the physical storage destination model for recordings.
 */
enum class StorageType(val displayName: String, val isPrivate: Boolean) {
    PRIVATE_APP("Private App Storage", true),
    SHARED_MEDIASTORE("Shared Device Media", false),
    CUSTOM_DOCUMENT("Custom Storage Location", false)
}

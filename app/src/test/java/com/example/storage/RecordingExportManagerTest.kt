package com.example.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingExportManagerTest {

    @Test
    fun resolveConflictName_whenNoConflict_returnsOriginalName() {
        val existing = setOf("other_video.mp4", "notes.txt")
        val result = RecordingExportManager.resolveConflictName(existing, "VID_20261029.mp4")
        assertEquals("VID_20261029.mp4", result)
    }

    @Test
    fun resolveConflictName_whenFileExists_appends1() {
        val existing = setOf("VID_20261029.mp4")
        val result = RecordingExportManager.resolveConflictName(existing, "VID_20261029.mp4")
        assertEquals("VID_20261029 (1).mp4", result)
    }

    @Test
    fun resolveConflictName_whenMultipleExist_incrementsCounter() {
        val existing = setOf(
            "AUD_20261029.m4a",
            "AUD_20261029 (1).m4a",
            "AUD_20261029 (2).m4a"
        )
        val result = RecordingExportManager.resolveConflictName(existing, "AUD_20261029.m4a")
        assertEquals("AUD_20261029 (3).m4a", result)
    }

    @Test
    fun resolveConflictName_preservesWavExtension() {
        val existing = setOf("AUD_20261029.wav")
        val result = RecordingExportManager.resolveConflictName(existing, "AUD_20261029.wav")
        assertEquals("AUD_20261029 (1).wav", result)
    }

    @Test
    fun resolveConflictName_withoutExtension_handlesCorrectly() {
        val existing = setOf("recording", "recording (1)")
        val result = RecordingExportManager.resolveConflictName(existing, "recording")
        assertEquals("recording (2)", result)
    }
}

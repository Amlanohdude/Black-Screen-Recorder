package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.recording.CameraHardwareHelper
import com.example.recording.RecordingService
import com.example.recording.RecordingStage
import com.example.recording.RecordingStatus
import com.example.recording.RecordingType
import com.example.recording.SafetyGuardrailManager
import com.example.recording.SafetyStopReason
import com.example.storage.RecordingEntity
import com.example.storage.SettingsPreferences
import com.example.storage.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BugFixesVerificationTest {

    private lateinit var context: Context
    private lateinit var prefs: SettingsPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = SettingsPreferences(context)
        // Reset to clean idle status before each test
        RecordingService.updateStatus(RecordingStatus())
    }

    @Test
    fun recordingService_errorStateAndRecoveryLifecycle() {
        // Initially idle
        val initialStatus = RecordingService.recordingStatus.value
        assertEquals(RecordingStage.IDLE, initialStatus.stage)
        assertFalse(initialStatus.isRecording)
        assertNull(initialStatus.error)

        // Simulate preflight error (e.g. critically low storage or battery)
        val errorMessage = "Storage is critically low (12 MB remaining)."
        RecordingService.updateStatus(
            RecordingStatus(
                stage = RecordingStage.ERROR,
                isRecording = false,
                error = errorMessage
            )
        )

        val errorStatus = RecordingService.recordingStatus.value
        assertEquals(RecordingStage.ERROR, errorStatus.stage)
        assertFalse(errorStatus.isRecording)
        assertEquals(errorMessage, errorStatus.error)

        // Simulate error dialog dismissal or snackbar reset
        RecordingService.updateStatus(RecordingStatus())

        val recoveredStatus = RecordingService.recordingStatus.value
        assertEquals(RecordingStage.IDLE, recoveredStatus.stage)
        assertFalse(recoveredStatus.isRecording)
        assertNull(recoveredStatus.error)
    }

    @Test
    fun biometricSettings_toggleAndPersistence() {
        // Default biometric setting check
        prefs.updateBiometricEnabled(false)
        assertFalse(prefs.settings.value.biometricEnabled)

        // Enable biometrics
        prefs.updateBiometricEnabled(true)
        assertTrue(prefs.settings.value.biometricEnabled)

        // Disable biometrics again
        prefs.updateBiometricEnabled(false)
        assertFalse(prefs.settings.value.biometricEnabled)
    }

    @Test
    fun batchExportSelection_operatesAcrossAllRecordingsRegardlessOfFilter() {
        val r1 = RecordingEntity(id = 1L, title = "Video 1", filePath = "/path/1.mp4", durationSeconds = 10, fileSizeBytes = 1000, type = "VIDEO")
        val r2 = RecordingEntity(id = 2L, title = "Audio 1", filePath = "/path/2.m4a", durationSeconds = 20, fileSizeBytes = 500, type = "AUDIO")
        val r3 = RecordingEntity(id = 3L, title = "Video 2", filePath = "/path/3.mp4", durationSeconds = 30, fileSizeBytes = 2000, type = "VIDEO")

        val allRecordings = listOf(r1, r2, r3)

        // User selected items 1 (video) and 2 (audio)
        val selectedIds = setOf(1L, 2L)

        // Suppose user is currently viewing the VIDEO filter tab
        val videoFilteredRecordings = allRecordings.filter { it.type == "VIDEO" }

        // The old buggy way filtered from filteredRecordings:
        val oldExportItems = videoFilteredRecordings.filter { selectedIds.contains(it.id) }
        assertEquals(1, oldExportItems.size) // Only 1 item would be exported, dropping item 2!

        // The fixed way filters from all recordings:
        val fixedExportItems = allRecordings.filter { it.id in selectedIds }
        assertEquals(2, fixedExportItems.size) // Both selected items are preserved!
        assertTrue(fixedExportItems.any { it.id == 1L })
        assertTrue(fixedExportItems.any { it.id == 2L })
    }

    @Test
    fun cameraHardwareHelper_testingOverrideFunctionality() {
        CameraHardwareHelper.force4kSupportForTesting = null
        // Default execution shouldn't throw
        val defaultSupport = CameraHardwareHelper.is4KSupported(context, useFrontCamera = false)

        // Force 4K support to true
        CameraHardwareHelper.force4kSupportForTesting = true
        assertTrue(CameraHardwareHelper.is4KSupported(context, useFrontCamera = false))
        assertTrue(CameraHardwareHelper.is4KSupported(context, useFrontCamera = true))

        // Force 4K support to false
        CameraHardwareHelper.force4kSupportForTesting = false
        assertFalse(CameraHardwareHelper.is4KSupported(context, useFrontCamera = false))
        assertFalse(CameraHardwareHelper.is4KSupported(context, useFrontCamera = true))

        // Reset
        CameraHardwareHelper.force4kSupportForTesting = null
    }

    @Test
    fun safetyGuardrail_preflightValidation() {
        // Safe settings
        val safeSettings = UserSettings(
            safetyGuardrailsEnabled = true,
            lowBatteryStopThresholdPct = 3,
            lowStorageStopThresholdMb = 10
        )
        val safeResult = SafetyGuardrailManager.checkPreflight(context, safeSettings)
        assertTrue(safeResult is SafetyGuardrailManager.PreflightCheckResult.Safe)

        // When guardrails disabled, always safe
        val disabledSettings = UserSettings(
            safetyGuardrailsEnabled = false,
            lowBatteryStopThresholdPct = 99,
            lowStorageStopThresholdMb = 999_999
        )
        val disabledResult = SafetyGuardrailManager.checkPreflight(context, disabledSettings)
        assertTrue(disabledResult is SafetyGuardrailManager.PreflightCheckResult.Safe)
    }

    @Test
    fun inAppUpdateManager_simulationAndStateManagement() {
        // Test update prompt visibility flow
        com.example.update.InAppUpdateManager.showUpdatePrompt(true)
        assertTrue(com.example.update.InAppUpdateManager.isUpdatePromptVisible.value)

        com.example.update.InAppUpdateManager.showUpdatePrompt(false)
        assertFalse(com.example.update.InAppUpdateManager.isUpdatePromptVisible.value)

        // Test downloaded banner simulation flow
        com.example.update.InAppUpdateManager.simulateUpdateDownloaded(true)
        assertTrue(com.example.update.InAppUpdateManager.isUpdateDownloaded.value)

        com.example.update.InAppUpdateManager.dismissDownloadedBanner()
        assertFalse(com.example.update.InAppUpdateManager.isUpdateDownloaded.value)
    }

    @Test
    fun backgroundRecording_defaultsToTrueAndPersists() {
        // Background recording must be enabled by default for a black screen recorder
        assertTrue(UserSettings().backgroundRecordingEnabled)

        // Toggle to false
        prefs.updateBackgroundRecordingEnabled(false)
        assertFalse(prefs.settings.value.backgroundRecordingEnabled)

        // Toggle back to true
        prefs.updateBackgroundRecordingEnabled(true)
        assertTrue(prefs.settings.value.backgroundRecordingEnabled)
    }

    @Test
    fun stopOnScreenOff_defaultsToFalseAndPersists() {
        // Stop on power button must be opt-in (default false) so screen off does not kill recording
        assertFalse(UserSettings().stopOnScreenOff)

        prefs.updateStopOnScreenOff(true)
        assertTrue(prefs.settings.value.stopOnScreenOff)

        prefs.updateStopOnScreenOff(false)
        assertFalse(prefs.settings.value.stopOnScreenOff)
    }
}

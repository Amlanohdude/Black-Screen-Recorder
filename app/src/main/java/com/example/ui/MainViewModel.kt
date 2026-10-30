package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.covers.CoverConfig
import com.example.presets.RecordingPresetId
import com.example.recording.RecordingService
import com.example.recording.RecordingStatus
import com.example.recording.RecordingType
import com.example.storage.AppDatabase
import com.example.storage.ExitGesture
import com.example.storage.ExitMethod
import com.example.storage.ExportProgress
import com.example.storage.ExportResult
import com.example.storage.RecordingEntity
import com.example.storage.RecordingRepository
import com.example.storage.SettingsPreferences
import com.example.storage.StopBehavior
import com.example.storage.StorageCapacityEstimator
import com.example.storage.StorageStats
import com.example.storage.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = RecordingRepository(db.recordingDao(), application)
    val settingsPrefs = SettingsPreferences(application)

    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingStatus: StateFlow<RecordingStatus> = RecordingService.recordingStatus

    val userSettings: StateFlow<UserSettings> = settingsPrefs.settings

    private val _storageStats = MutableStateFlow(
        StorageStats(
            availableBytes = 42_800_000_000L,
            totalBytes = 64_000_000_000L,
            usedBytes = 0L,
            isPrivateStorage = true
        )
    )
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    val savedCustomCovers: StateFlow<List<CoverConfig>> = settingsPrefs.savedCustomCovers

    private val _coverToCustomize = MutableStateFlow<CoverConfig?>(null)
    val coverToCustomize: StateFlow<CoverConfig?> = _coverToCustomize.asStateFlow()

    private val _isRecordingsUnlocked = MutableStateFlow(false)
    val isRecordingsUnlocked: StateFlow<Boolean> = _isRecordingsUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.reconcileOrphanFiles()
            val settings = userSettings.value
            if (settings.autoDeleteEnabled && settings.autoDeleteDays > 0) {
                repository.purgeRecordingsOlderThan(settings.autoDeleteDays)
            }
            refreshStorageStats()
        }
        viewModelScope.launch {
            recordings.collect {
                refreshStorageStats()
            }
        }
        viewModelScope.launch {
            recordingStatus.collect { status ->
                if (status.isRecording) {
                    refreshStorageStats()
                }
            }
        }
    }

    fun reconcileOrphanFiles() {
        viewModelScope.launch {
            repository.reconcileOrphanFiles()
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = StorageCapacityEstimator.getStorageStats(getApplication())
            _storageStats.value = stats
        }
    }

    fun runAutoDeleteCheck() {
        viewModelScope.launch {
            val settings = userSettings.value
            if (settings.autoDeleteEnabled && settings.autoDeleteDays > 0) {
                repository.purgeRecordingsOlderThan(settings.autoDeleteDays)
            }
        }
    }

    fun startAudio(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_AUDIO
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun startVideo(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_VIDEO
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun pauseRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun saveCompletedVideo(context: Context, file: File, durationSeconds: Long) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_VIDEO_SAVED
            putExtra(RecordingService.EXTRA_FILE_PATH, file.absolutePath)
            putExtra(RecordingService.EXTRA_DURATION, durationSeconds)
        }
        context.startService(intent)
    }

    fun getBatchShareIntent(recordings: List<RecordingEntity>): Intent? {
        return repository.getBatchShareIntent(recordings)
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(recording, newTitle)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }

    fun deleteRecordingByFilePath(filePath: String) {
        viewModelScope.launch {
            repository.deleteRecordingByFilePath(filePath)
        }
    }

    fun deleteRecordings(recordingsList: List<RecordingEntity>) {
        viewModelScope.launch {
            repository.deleteRecordings(recordingsList)
        }
    }

    suspend fun exportRecordingToCustomFolder(
        recording: RecordingEntity,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult {
        val folderUri = userSettings.value.customExportFolderUri
        return repository.exportRecordingToCustomFolder(recording, folderUri, onProgress, isCancelled)
    }

    suspend fun exportBatchToCustomFolder(
        recordingsList: List<RecordingEntity>,
        onProgress: (ExportProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): ExportResult {
        val folderUri = userSettings.value.customExportFolderUri
        return repository.exportBatchToCustomFolder(recordingsList, folderUri, onProgress, isCancelled)
    }

    fun updateCamera(useFront: Boolean) = settingsPrefs.updateCamera(useFront)
    fun updateAudioFormat(format: String) = settingsPrefs.updateAudioFormat(format)
    fun updateVideoQuality(quality: String) = settingsPrefs.updateVideoQuality(quality)
    fun updatePureBlackScreen(pureBlack: Boolean) = settingsPrefs.updatePureBlackScreen(pureBlack)
    fun updateKeepScreenAwake(keepAwake: Boolean) = settingsPrefs.updateKeepScreenAwake(keepAwake)
    fun updateDefaultRecordingType(type: String) = settingsPrefs.updateDefaultRecordingType(type)
    fun setActiveCover(cover: CoverConfig) = settingsPrefs.updateActiveCover(cover)
    fun saveCustomCover(cover: CoverConfig) = settingsPrefs.saveCustomCover(cover)
    fun deleteCustomCover(coverId: String) = settingsPrefs.deleteCustomCover(coverId)

    fun openCoverDesigner(cover: CoverConfig) {
        _coverToCustomize.value = cover
    }

    fun closeCoverDesigner() {
        _coverToCustomize.value = null
    }

    fun updateAutoDeleteEnabled(enabled: Boolean) {
        settingsPrefs.updateAutoDeleteEnabled(enabled)
        if (enabled) runAutoDeleteCheck()
    }

    fun updateAutoDeleteDays(days: Int) {
        settingsPrefs.updateAutoDeleteDays(days)
        if (userSettings.value.autoDeleteEnabled) runAutoDeleteCheck()
    }

    fun updateAutoStopSeconds(seconds: Int) = settingsPrefs.updateAutoStopSeconds(seconds)
    fun updateAutoStopMinutes(minutes: Int) = settingsPrefs.updateAutoStopMinutes(minutes)
    fun updateLanguageCode(code: String) = settingsPrefs.updateLanguageCode(code)
    fun updateStopBehavior(behavior: StopBehavior) = settingsPrefs.updateStopBehavior(behavior)
    fun updateHapticFeedbackEnabled(enabled: Boolean) = settingsPrefs.updateHapticFeedbackEnabled(enabled)
    fun updateExitAfterRecordingEnabled(enabled: Boolean) = settingsPrefs.updateExitAfterRecordingEnabled(enabled)
    fun updateBackgroundRecordingEnabled(enabled: Boolean) = settingsPrefs.updateBackgroundRecordingEnabled(enabled)
    fun updateExitMethod(method: ExitMethod) = settingsPrefs.updateExitMethod(method)
    fun updateExitGesture(gesture: ExitGesture) = settingsPrefs.updateExitGesture(gesture)
    fun updateQuickStopAndExit(enabled: Boolean) = settingsPrefs.updateQuickStopAndExit(enabled)
    fun applyPreset(presetId: RecordingPresetId) = settingsPrefs.applyPreset(presetId)
    fun updatePreferredMicrophone(micId: String) = settingsPrefs.updatePreferredMicrophone(micId)
    fun updateAudioQuality(quality: String) = settingsPrefs.updateAudioQuality(quality)
    fun updateAudioClearVoice(enabled: Boolean) = settingsPrefs.updateAudioClearVoice(enabled)
    fun updateAudioReduceEcho(enabled: Boolean) = settingsPrefs.updateAudioReduceEcho(enabled)
    fun updateAudioInputVolume(volume: Float) = settingsPrefs.updateAudioInputVolume(volume)
    fun updateAudioChannels(channels: Int) = settingsPrefs.updateAudioChannels(channels)
    fun updateAudioBitrate(bitrate: Int) = settingsPrefs.updateAudioBitrate(bitrate)
    fun updateAudioSampleRate(sampleRate: Int) = settingsPrefs.updateAudioSampleRate(sampleRate)
    fun updateSafetyGuardrailsEnabled(enabled: Boolean) = settingsPrefs.updateSafetyGuardrailsEnabled(enabled)
    fun updateLowBatteryStopThresholdPct(threshold: Int) = settingsPrefs.updateLowBatteryStopThresholdPct(threshold)
    fun updateLowStorageStopThresholdMb(threshold: Int) = settingsPrefs.updateLowStorageStopThresholdMb(threshold)

    fun unlockRecordings() {
        _isRecordingsUnlocked.value = true
    }

    fun lockRecordings() {
        _isRecordingsUnlocked.value = false
    }

    fun verifyPin(input: String): Boolean {
        return settingsPrefs.verifyPin(input)
    }

    fun setPin(pin: String, enableBiometric: Boolean = false) {
        settingsPrefs.setPin(pin, enableBiometric)
    }

    fun clearPin() {
        settingsPrefs.clearPin()
    }

    fun updateLockEnabled(enabled: Boolean) {
        settingsPrefs.updateLockEnabled(enabled)
    }

    fun updateBiometricEnabled(enabled: Boolean) {
        settingsPrefs.updateBiometricEnabled(enabled)
    }

    fun updateCustomExportFolder(uri: String?, folderName: String?) {
        if (uri != null && folderName != null) {
            settingsPrefs.updateCustomExportFolder(uri, folderName)
        } else {
            settingsPrefs.clearCustomExportFolder()
        }
    }

    fun clearCustomExportFolder() {
        settingsPrefs.clearCustomExportFolder()
    }

    fun completeOnboarding() {
        settingsPrefs.completeOnboarding()
    }

    fun markFirstRecordingExplanationShown() {
        settingsPrefs.markFirstRecordingExplanationShown()
    }

    fun resetOnboardingState() {
        settingsPrefs.resetOnboardingState()
    }

    companion object {
        fun formatDuration(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatTimestamp(timestamp: Long): String {
            val now = Calendar.getInstance()
            val time = Calendar.getInstance().apply { timeInMillis = timestamp }
            return if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)
            ) {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            } else if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
            } else {
                SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}

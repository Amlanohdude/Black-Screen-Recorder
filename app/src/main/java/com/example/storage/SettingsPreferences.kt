package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.covers.ClockStyle
import com.example.covers.ControlGesture
import com.example.covers.CoverBackground
import com.example.covers.CoverConfig
import com.example.covers.CoverPresetId
import com.example.covers.DateFormatStyle
import com.example.presets.RecordingPresetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

enum class StopBehavior(val displayName: String, val description: String) {
    HOLD_TO_STOP("Hold to stop", "Hold for ~1s to end recording (Prevents accidental stop)"),
    TAP_TO_STOP("Tap to stop", "Single tap ends recording immediately"),
    CONFIRM_TO_STOP("Confirm before stopping", "Prompt confirmation dialog before stopping")
}

enum class ExitMethod(val id: String, val title: String, val description: String) {
    NORMAL_STOP_CONTROLS("NORMAL_STOP_CONTROLS", "Use normal stop controls", "Stop, save, and exit using the standard stop controls."),
    GESTURE("GESTURE", "Use a gesture", "Use a discreet gesture while recording to stop, save, and exit.");

    companion object {
        fun fromId(id: String?): ExitMethod {
            return entries.firstOrNull { it.id == id || it.name == id } ?: NORMAL_STOP_CONTROLS
        }
    }
}

enum class ExitGesture(val id: String, val title: String, val description: String, val shortSubtitle: String) {
    TWO_FINGER_DOUBLE_TAP("TWO_FINGER_DOUBLE_TAP", "Two-finger double-tap (Recommended)", "Double-tap the recording screen with two fingers.", "Two-finger double-tap"),
    THREE_FINGER_LONG_PRESS("THREE_FINGER_LONG_PRESS", "Three-finger long press", "Press and hold the recording screen with three fingers.", "Three-finger long press"),
    DOUBLE_TAP_CORNER("DOUBLE_TAP_CORNER", "Double-tap lower-right corner", "Double-tap the lower-right area of the recording screen.", "Double-tap lower-right"),
    VOLUME_DOWN_TWICE("VOLUME_DOWN_TWICE", "Volume Down twice", "Press Volume Down twice while recording. Support may vary by device.", "Volume Down twice");

    companion object {
        fun fromId(id: String?): ExitGesture {
            return entries.firstOrNull { it.id == id || it.name == id } ?: TWO_FINGER_DOUBLE_TAP
        }
    }
}

data class UserSettings(
    val useFrontCamera: Boolean = false,
    val videoQuality: String = "1080p", // "1080p", "720p", "480p"
    val pureBlackScreen: Boolean = true, // Legacy compatibility
    val keepScreenAwake: Boolean = true,
    val audioFormat: String = "M4A (AAC)",
    val defaultRecordingType: String = "VIDEO", // "VIDEO" or "AUDIO"
    val activeCover: CoverConfig = CoverConfig.PURE_BLACK_PRESET,
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteDays: Int = 7,
    val autoStopSeconds: Int = 0, // 0 = Off
    val stopBehavior: StopBehavior = StopBehavior.HOLD_TO_STOP,
    val activePreset: RecordingPresetId = RecordingPresetId.DISCREET_VIDEO,
    val lockEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val biometricEnabled: Boolean = false,
    val customExportFolderUri: String? = null,
    val customExportFolderName: String? = null,
    val exitAfterRecordingEnabled: Boolean = false,
    val backgroundRecordingEnabled: Boolean = false,
    val exitMethod: ExitMethod = ExitMethod.NORMAL_STOP_CONTROLS,
    val exitGesture: ExitGesture = ExitGesture.TWO_FINGER_DOUBLE_TAP,
    val preferredMicrophone: String = "PHONE_MIC",
    val audioQuality: String = "HIGH",
    val audioClearVoice: Boolean = true,
    val audioReduceEcho: Boolean = true,
    val audioInputVolume: Float = 1.0f,
    val audioChannels: Int = 2,
    val audioBitrate: Int = 192000,
    val audioSampleRate: Int = 44100,
    val activeAudioPreset: RecordingPresetId = RecordingPresetId.GENERAL_AUDIO,
    val activeVideoPreset: RecordingPresetId = RecordingPresetId.DISCREET_VIDEO,
    val onboardingCompleted: Boolean = true,
    val firstRecordingExplanationShown: Boolean = false
) {
    val quickStopAndExitEnabled: Boolean get() = exitAfterRecordingEnabled
    val autoStopMinutes: Int get() = autoStopSeconds / 60
    val hasCustomExportFolder: Boolean get() = !customExportFolderUri.isNullOrEmpty()
}

class SettingsPreferences(context: Context) {
private val prefs: SharedPreferences = context.getSharedPreferences("bsr_prefs", Context.MODE_PRIVATE)

private val _savedCustomCovers = MutableStateFlow(loadSavedCustomCovers())
val savedCustomCovers: StateFlow<List<CoverConfig>> = _savedCustomCovers.asStateFlow()

private val _settings = MutableStateFlow(loadSettings())
val settings: StateFlow<UserSettings> = _settings.asStateFlow()

private fun loadSavedCustomCovers(): List<CoverConfig> {
val jsonStr = prefs.getString("custom_saved_covers", null)
return CoverConfig.listFromJson(jsonStr)
}

private fun loadSettings(): UserSettings {
val activeCoverJsonStr = prefs.getString("active_cover_json", null)
val jsonCover = if (!activeCoverJsonStr.isNullOrBlank()) {
try {
CoverConfig.fromJson(org.json.JSONObject(activeCoverJsonStr))
} catch (_: Exception) {
null
}
} else null

val activeCover = jsonCover ?: run {
val presetStr = prefs.getString("cover_preset_id", CoverPresetId.PURE_BLACK.name) ?: CoverPresetId.PURE_BLACK.name
val presetId = try { CoverPresetId.valueOf(presetStr) } catch (_: Exception) { CoverPresetId.PURE_BLACK }
val baseConfig = CoverConfig.forPreset(presetId)

baseConfig.copy(
presetId = presetId,
name = prefs.getString("cover_name", baseConfig.name) ?: baseConfig.name,
showClock = prefs.getBoolean("cover_show_clock", baseConfig.showClock),
clockIs24Hour = prefs.getBoolean("cover_clock_24h", baseConfig.clockIs24Hour),
clockStyle = try {
ClockStyle.valueOf(prefs.getString("cover_clock_style", baseConfig.clockStyle.name) ?: baseConfig.clockStyle.name)
} catch (_: Exception) { baseConfig.clockStyle },
clockSizeSp = prefs.getInt("cover_clock_size", baseConfig.clockSizeSp),
clockOpacity = prefs.getFloat("cover_clock_opacity", baseConfig.clockOpacity),
showDate = prefs.getBoolean("cover_show_date", baseConfig.showDate),
dateFormat = try {
DateFormatStyle.valueOf(prefs.getString("cover_date_format", baseConfig.dateFormat.name) ?: baseConfig.dateFormat.name)
} catch (_: Exception) { baseConfig.dateFormat },
dateOpacity = prefs.getFloat("cover_date_opacity", baseConfig.dateOpacity),
showBattery = prefs.getBoolean("cover_show_battery", baseConfig.showBattery),
background = try {
CoverBackground.valueOf(prefs.getString("cover_background", baseConfig.background.name) ?: baseConfig.background.name)
} catch (_: Exception) { baseConfig.background },
brightnessPercent = prefs.getInt("cover_brightness", baseConfig.brightnessPercent),
showRecordingTimer = prefs.getBoolean("cover_show_timer", false),
controlGesture = try {
ControlGesture.valueOf(prefs.getString("cover_control_gesture", ControlGesture.SINGLE_TAP.name) ?: ControlGesture.SINGLE_TAP.name)
} catch (_: Exception) { ControlGesture.SINGLE_TAP }
)
}

        val preferredMicrophone = prefs.getString("preferred_microphone", "PHONE_MIC") ?: "PHONE_MIC"
        val audioQuality = prefs.getString("audio_quality", "HIGH") ?: "HIGH"
        val audioClearVoice = prefs.getBoolean("audio_clear_voice", true)
        val audioReduceEcho = prefs.getBoolean("audio_reduce_echo", true)
        val audioInputVolume = prefs.getFloat("audio_input_volume", 1.0f)
        val audioChannels = prefs.getInt("audio_channels", 2)
        val audioBitrate = prefs.getInt("audio_bitrate", 192000)
        val audioSampleRate = prefs.getInt("audio_sample_rate", 44100)
        val exitAfterRecordingEnabled = prefs.getBoolean("exit_after_recording_enabled", prefs.getBoolean("quick_stop_and_exit_enabled", false))
        val backgroundRecordingEnabled = prefs.getBoolean("background_recording_enabled", false)
        val exitMethod = ExitMethod.fromId(prefs.getString("exit_method", "NORMAL_STOP_CONTROLS"))
        val exitGesture = ExitGesture.fromId(prefs.getString("exit_gesture", "TWO_FINGER_DOUBLE_TAP"))
        val activeAudioPreset = try {
            RecordingPresetId.valueOf(prefs.getString("active_audio_preset", "GENERAL_AUDIO") ?: "GENERAL_AUDIO")
        } catch (_: Exception) {
            RecordingPresetId.GENERAL_AUDIO
        }
        val activeVideoPreset = try {
            RecordingPresetId.valueOf(prefs.getString("active_video_preset", "DISCREET_VIDEO") ?: "DISCREET_VIDEO")
        } catch (_: Exception) {
            RecordingPresetId.DISCREET_VIDEO
        }
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", true)
        val firstRecordingExplanationShown = prefs.getBoolean("first_recording_explanation_shown", false)

        return UserSettings(
            useFrontCamera = prefs.getBoolean("use_front_camera", false),
            videoQuality = prefs.getString("video_quality", "1080p") ?: "1080p",
            pureBlackScreen = prefs.getBoolean("pure_black_screen", true),
            keepScreenAwake = prefs.getBoolean("keep_screen_awake", true),
            audioFormat = prefs.getString("audio_format", "M4A (AAC)") ?: "M4A (AAC)",
            defaultRecordingType = prefs.getString("default_recording_type", "VIDEO") ?: "VIDEO",
            activeCover = activeCover,
            autoDeleteEnabled = prefs.getBoolean("auto_delete_enabled", false),
            autoDeleteDays = prefs.getInt("auto_delete_days", 7),
            autoStopSeconds = prefs.getInt("auto_stop_seconds", 0),
            stopBehavior = try {
                StopBehavior.valueOf(prefs.getString("stop_behavior", StopBehavior.HOLD_TO_STOP.name) ?: StopBehavior.HOLD_TO_STOP.name)
            } catch (_: Exception) {
                StopBehavior.HOLD_TO_STOP
            },
            activePreset = try {
                RecordingPresetId.valueOf(prefs.getString("active_preset", RecordingPresetId.DISCREET_VIDEO.name) ?: RecordingPresetId.DISCREET_VIDEO.name)
            } catch (_: Exception) {
                RecordingPresetId.DISCREET_VIDEO
            },
            lockEnabled = prefs.getBoolean("lock_enabled", false),
            hasPinSet = prefs.getString("lock_pin_hash", null) != null,
            biometricEnabled = prefs.getBoolean("biometric_enabled", false),
            customExportFolderUri = prefs.getString("custom_export_folder_uri", null),
            customExportFolderName = prefs.getString("custom_export_folder_name", null),
            exitAfterRecordingEnabled = exitAfterRecordingEnabled,
            backgroundRecordingEnabled = backgroundRecordingEnabled,
            exitMethod = exitMethod,
            exitGesture = exitGesture,
            preferredMicrophone = preferredMicrophone,
            audioQuality = audioQuality,
            audioClearVoice = audioClearVoice,
            audioReduceEcho = audioReduceEcho,
            audioInputVolume = audioInputVolume,
            audioChannels = audioChannels,
            audioBitrate = audioBitrate,
            audioSampleRate = audioSampleRate,
            activeAudioPreset = activeAudioPreset,
            activeVideoPreset = activeVideoPreset,
            onboardingCompleted = onboardingCompleted,
            firstRecordingExplanationShown = firstRecordingExplanationShown
        )
    }

    fun applyPreset(presetId: RecordingPresetId) {
        if (presetId.isAudioPreset()) {
            if (presetId == RecordingPresetId.CUSTOM || presetId == RecordingPresetId.CUSTOM_AUDIO) {
                prefs.edit()
                    .putString("active_preset", "CUSTOM_AUDIO")
                    .putString("active_audio_preset", "CUSTOM_AUDIO")
                    .apply()
                _settings.value = _settings.value.copy(
                    activePreset = RecordingPresetId.CUSTOM_AUDIO,
                    activeAudioPreset = RecordingPresetId.CUSTOM_AUDIO
                )
                return
            }
            val def = RecordingPresetId.getPresetDefinition(presetId)
            val bitrate = if (def.audioQuality == "STANDARD") 128000 else if (def.audioQuality == "MAXIMUM") 256000 else 192000
            val sampleRate = if (def.audioQuality == "STANDARD") 44100 else 48000
            prefs.edit()
                .putString("active_preset", presetId.name)
                .putString("active_audio_preset", presetId.name)
                .putString("default_recording_type", "AUDIO")
                .putString("audio_quality", def.audioQuality)
                .putBoolean("audio_clear_voice", def.audioClearVoice)
                .putBoolean("audio_reduce_echo", def.audioReduceEcho)
                .putInt("audio_channels", def.audioChannels)
                .putInt("audio_bitrate", bitrate)
                .putInt("audio_sample_rate", sampleRate)
                .apply()

            _settings.value = _settings.value.copy(
                defaultRecordingType = "AUDIO",
                activePreset = presetId,
                activeAudioPreset = presetId,
                audioQuality = def.audioQuality,
                audioClearVoice = def.audioClearVoice,
                audioReduceEcho = def.audioReduceEcho,
                audioChannels = def.audioChannels,
                audioBitrate = bitrate,
                audioSampleRate = sampleRate
            )
            return
        }

        if (presetId == RecordingPresetId.CUSTOM) {
            prefs.edit()
                .putString("active_preset", "CUSTOM")
                .putString("active_video_preset", "CUSTOM")
                .apply()
            _settings.value = _settings.value.copy(
                activePreset = RecordingPresetId.CUSTOM,
                activeVideoPreset = RecordingPresetId.CUSTOM
            )
            return
        }

        val def = RecordingPresetId.getPresetDefinition(presetId)
        val cover = def.coverConfig
        prefs.edit()
            .putString("active_preset", presetId.name)
            .putString("active_video_preset", presetId.name)
            .putString("default_recording_type", def.recordingType)
            .putString("video_quality", def.videoQuality)
            .putBoolean("use_front_camera", def.useFrontCamera)
            .putString("cover_preset_id", cover.presetId.name)
            .putString("cover_name", cover.name)
            .putBoolean("cover_show_clock", cover.showClock)
            .putBoolean("cover_clock_24h", cover.clockIs24Hour)
            .putString("cover_clock_style", cover.clockStyle.name)
            .putInt("cover_clock_size", cover.clockSizeSp)
            .putFloat("cover_clock_opacity", cover.clockOpacity)
            .putBoolean("cover_show_date", cover.showDate)
            .putString("cover_date_format", cover.dateFormat.name)
            .putFloat("cover_date_opacity", cover.dateOpacity)
            .putBoolean("cover_show_battery", cover.showBattery)
            .putString("cover_background", cover.background.name)
            .putInt("cover_brightness", cover.brightnessPercent)
            .putBoolean("cover_show_timer", cover.showRecordingTimer)
            .putString("cover_control_gesture", cover.controlGesture.name)
            .apply()

        _settings.value = _settings.value.copy(
            activePreset = presetId,
            activeVideoPreset = presetId,
            useFrontCamera = def.useFrontCamera,
            videoQuality = def.videoQuality,
            defaultRecordingType = def.recordingType,
            activeCover = cover
        )
    }

fun updateCamera(useFront: Boolean) {
prefs.edit().putBoolean("use_front_camera", useFront).apply()
_settings.value = _settings.value.copy(useFrontCamera = useFront)
}

fun updateAudioFormat(format: String) {
prefs.edit().putString("audio_format", format).apply()
_settings.value = _settings.value.copy(audioFormat = format)
}

fun updateVideoQuality(quality: String) {
prefs.edit().putString("video_quality", quality).apply()
_settings.value = _settings.value.copy(videoQuality = quality)
}

fun updatePureBlackScreen(pureBlack: Boolean) {
prefs.edit().putBoolean("pure_black_screen", pureBlack).apply()
_settings.value = _settings.value.copy(pureBlackScreen = pureBlack)
}

fun updateKeepScreenAwake(keepAwake: Boolean) {
prefs.edit().putBoolean("keep_screen_awake", keepAwake).apply()
_settings.value = _settings.value.copy(keepScreenAwake = keepAwake)
}

fun updateDefaultRecordingType(type: String) {
    val isAudio = (type == "AUDIO")
    val targetPreset = if (isAudio) _settings.value.activeAudioPreset else _settings.value.activeVideoPreset
    prefs.edit()
        .putString("default_recording_type", type)
        .putString("active_preset", targetPreset.name)
        .apply()
    _settings.value = _settings.value.copy(
        defaultRecordingType = type,
        activePreset = targetPreset
    )
}

fun updateActiveCover(cover: CoverConfig) {
prefs.edit()
.putString("active_cover_json", cover.toJson().toString())
.putString("cover_preset_id", cover.presetId.name)
.putString("cover_name", cover.name)
.putBoolean("cover_show_clock", cover.showClock)
.putBoolean("cover_clock_24h", cover.clockIs24Hour)
.putString("cover_clock_style", cover.clockStyle.name)
.putInt("cover_clock_size", cover.clockSizeSp)
.putFloat("cover_clock_opacity", cover.clockOpacity)
.putBoolean("cover_show_date", cover.showDate)
.putString("cover_date_format", cover.dateFormat.name)
.putFloat("cover_date_opacity", cover.dateOpacity)
.putBoolean("cover_show_battery", cover.showBattery)
.putString("cover_background", cover.background.name)
.putInt("cover_brightness", cover.brightnessPercent)
.putBoolean("cover_show_timer", cover.showRecordingTimer)
.putString("cover_control_gesture", cover.controlGesture.name)
.apply()

_settings.value = _settings.value.copy(activeCover = cover)
}

fun saveCustomCover(cover: CoverConfig) {
val current = _savedCustomCovers.value.toMutableList()
val index = current.indexOfFirst { it.id == cover.id }
if (index >= 0) {
current[index] = cover
} else {
current.add(cover)
}
prefs.edit().putString("custom_saved_covers", CoverConfig.listToJson(current)).apply()
_savedCustomCovers.value = current
if (_settings.value.activeCover.id == cover.id) {
updateActiveCover(cover)
}
}

fun deleteCustomCover(coverId: String) {
val current = _savedCustomCovers.value.toMutableList()
current.removeAll { it.id == coverId }
prefs.edit().putString("custom_saved_covers", CoverConfig.listToJson(current)).apply()
_savedCustomCovers.value = current
if (_settings.value.activeCover.id == coverId) {
updateActiveCover(CoverConfig.PURE_BLACK_PRESET)
}
}


fun updateAutoDeleteEnabled(enabled: Boolean) {
prefs.edit().putBoolean("auto_delete_enabled", enabled).apply()
_settings.value = _settings.value.copy(autoDeleteEnabled = enabled)
}

fun updateAutoDeleteDays(days: Int) {
prefs.edit().putInt("auto_delete_days", days).apply()
_settings.value = _settings.value.copy(autoDeleteDays = days)
}

fun updateAutoStopSeconds(seconds: Int) {
    val safeSeconds = seconds.coerceAtLeast(0)
    prefs.edit().putInt("auto_stop_seconds", safeSeconds).putInt("auto_stop_minutes", safeSeconds / 60).apply()
    _settings.value = _settings.value.copy(autoStopSeconds = safeSeconds)
}

fun updateAutoStopMinutes(minutes: Int) {
    updateAutoStopSeconds(minutes * 60)
}

fun updateStopBehavior(behavior: StopBehavior) {
    prefs.edit().putString("stop_behavior", behavior.name).apply()
    _settings.value = _settings.value.copy(stopBehavior = behavior)
}

fun updateLockEnabled(enabled: Boolean) {
if (enabled && !_settings.value.hasPinSet) return
prefs.edit().putBoolean("lock_enabled", enabled).apply()
_settings.value = _settings.value.copy(lockEnabled = enabled)
}

fun setPin(pin: String, enableBiometric: Boolean = false) {
val saltBytes = ByteArray(16)
SecureRandom().nextBytes(saltBytes)
val salt = saltBytes.joinToString("") { "%02x".format(it) }
val hash = hashWithSalt(pin, salt)

prefs.edit()
.putString("lock_pin_hash", hash)
.putString("lock_pin_salt", salt)
.remove("lock_pin")
.putBoolean("lock_enabled", true)
.putBoolean("biometric_enabled", enableBiometric)
.apply()

_settings.value = _settings.value.copy(
hasPinSet = true,
lockEnabled = true,
biometricEnabled = enableBiometric
)
}

fun verifyPin(pin: String): Boolean {
val storedHash = prefs.getString("lock_pin_hash", null) ?: return false
val salt = prefs.getString("lock_pin_salt", null) ?: return false
val computedHash = hashWithSalt(pin, salt)
return MessageDigest.isEqual(
computedHash.toByteArray(Charsets.UTF_8),
storedHash.toByteArray(Charsets.UTF_8)
)
}

fun clearPin() {
prefs.edit()
.remove("lock_pin_hash")
.remove("lock_pin_salt")
.remove("lock_pin")
.putBoolean("lock_enabled", false)
.putBoolean("biometric_enabled", false)
.apply()

_settings.value = _settings.value.copy(
hasPinSet = false,
lockEnabled = false,
biometricEnabled = false
)
}

fun updateBiometricEnabled(enabled: Boolean) {
prefs.edit().putBoolean("biometric_enabled", enabled).apply()
_settings.value = _settings.value.copy(biometricEnabled = enabled)
}

    fun updateCustomExportFolder(uri: String?, folderName: String?) {
        prefs.edit()
            .putString("custom_export_folder_uri", uri)
            .putString("custom_export_folder_name", folderName)
            .apply()
        _settings.value = _settings.value.copy(
            customExportFolderUri = uri,
            customExportFolderName = folderName
        )
    }

    fun clearCustomExportFolder() {
prefs.edit()
.remove("custom_export_folder_uri")
.remove("custom_export_folder_name")
.apply()
_settings.value = _settings.value.copy(
customExportFolderUri = null,
customExportFolderName = null
)
}

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _settings.value = _settings.value.copy(onboardingCompleted = true)
    }

    fun markFirstRecordingExplanationShown() {
        prefs.edit().putBoolean("first_recording_explanation_shown", true).apply()
    }

    fun resetOnboardingState() {
        prefs.edit()
            .putBoolean("onboarding_completed", false)
            .putBoolean("first_recording_explanation_shown", false)
            .apply()
        _settings.value = _settings.value.copy(onboardingCompleted = false)
    }

    private fun hashWithSalt(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun updatePreferredMicrophone(micId: String) {
        prefs.edit().putString("preferred_microphone", micId).apply()
        _settings.value = _settings.value.copy(preferredMicrophone = micId)
    }

    fun updateAudioQuality(quality: String) {
        val upper = quality.uppercase(Locale.ROOT)
        val bitrate = if (upper == "STANDARD") 128000 else if (upper == "MAXIMUM") 256000 else 192000
        val sampleRate = if (upper == "STANDARD") 44100 else 48000
        prefs.edit().putString("audio_quality", quality).putInt("audio_bitrate", bitrate).putInt("audio_sample_rate", sampleRate).apply()
        _settings.value = _settings.value.copy(audioQuality = quality, audioBitrate = bitrate, audioSampleRate = sampleRate)
    }

    fun updateAudioClearVoice(enabled: Boolean) {
        prefs.edit().putBoolean("audio_clear_voice", enabled).apply()
        _settings.value = _settings.value.copy(audioClearVoice = enabled)
    }

    fun updateAudioReduceEcho(enabled: Boolean) {
        prefs.edit().putBoolean("audio_reduce_echo", enabled).apply()
        _settings.value = _settings.value.copy(audioReduceEcho = enabled)
    }

    fun updateAudioInputVolume(volume: Float) {
        val clamped = volume.coerceIn(0.5f, 1.5f)
        prefs.edit().putFloat("audio_input_volume", clamped).apply()
        _settings.value = _settings.value.copy(audioInputVolume = clamped)
    }

    fun updateAudioChannels(channels: Int) {
        prefs.edit().putInt("audio_channels", channels).apply()
        _settings.value = _settings.value.copy(audioChannels = channels)
    }

    fun updateAudioBitrate(bitrate: Int) {
        prefs.edit().putInt("audio_bitrate", bitrate).apply()
        _settings.value = _settings.value.copy(audioBitrate = bitrate)
    }

    fun updateAudioSampleRate(sampleRate: Int) {
        prefs.edit().putInt("audio_sample_rate", sampleRate).apply()
        _settings.value = _settings.value.copy(audioSampleRate = sampleRate)
    }

    fun updateExitAfterRecordingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("exit_after_recording_enabled", enabled).putBoolean("quick_stop_and_exit_enabled", enabled).apply()
        _settings.value = _settings.value.copy(exitAfterRecordingEnabled = enabled)
    }

    fun updateBackgroundRecordingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("background_recording_enabled", enabled).apply()
        _settings.value = _settings.value.copy(backgroundRecordingEnabled = enabled)
    }

    fun updateExitMethod(method: ExitMethod) {
        prefs.edit().putString("exit_method", method.name).apply()
        _settings.value = _settings.value.copy(exitMethod = method)
    }

    fun updateExitGesture(gesture: ExitGesture) {
        prefs.edit().putString("exit_gesture", gesture.name).apply()
        _settings.value = _settings.value.copy(exitGesture = gesture)
    }

    fun updateQuickStopAndExit(enabled: Boolean) {
        updateExitAfterRecordingEnabled(enabled)
    }



    companion object {
        fun formatAutoStopDuration(totalSeconds: Int): String {
            if (totalSeconds <= 0) return "Off"
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return if (secs == 0) "${mins}m" else "${mins}m ${secs}s"
        }

        fun formatAutoStopShort(totalSeconds: Int): String {
            if (totalSeconds <= 0) return "Off"
            val mins = totalSeconds / 60
            return "${mins}m"
        }
    }
}
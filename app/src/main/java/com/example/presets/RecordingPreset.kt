package com.example.presets

import com.example.covers.CoverConfig
import com.example.covers.CoverPresetId

enum class RecordingPresetId(
    val title: String,
    val description: String,
    val shortSubtitle: String
) {
    DISCREET_VIDEO(
        title = "Discreet Video",
        description = "720p · Back camera · Pure Black · Minimum brightness",
        shortSubtitle = "720p · Back cam · Pure Black · Min brightness"
    ),
    STANDARD_VIDEO(
        title = "Standard",
        description = "1080p · Back camera · Clock",
        shortSubtitle = "1080p · Back cam · Clock cover"
    ),
    VOICE_AUDIO(
        title = "Voice",
        description = "Best for conversations and speech",
        shortSubtitle = "Voice · Clear Voice · Phone mic"
    ),
    GENERAL_AUDIO(
        title = "General",
        description = "Balanced quality and file size",
        shortSubtitle = "Standard quality · Balanced"
    ),
    HIGH_QUALITY_AUDIO(
        title = "High Quality",
        description = "Best sound quality",
        shortSubtitle = "Maximum quality · High fidelity"
    ),
    CUSTOM_AUDIO(
        title = "Custom",
        description = "Custom microphone & audio quality setup",
        shortSubtitle = "Custom audio settings"
    ),
    AUDIO(
        title = "Audio",
        description = "Audio only · Pure Black",
        shortSubtitle = "Audio only · Pure Black"
    ),
    CUSTOM(
        title = "Custom",
        description = "Custom resolution, camera & cover setup",
        shortSubtitle = "Custom settings"
    );

    fun isAudioPreset(): Boolean =
        this == VOICE_AUDIO || this == GENERAL_AUDIO || this == HIGH_QUALITY_AUDIO || this == CUSTOM_AUDIO || this == AUDIO

    fun isVideoPreset(): Boolean =
        this == DISCREET_VIDEO || this == STANDARD_VIDEO || this == CUSTOM

    companion object {
        fun getPresetsForMode(isAudio: Boolean): List<RecordingPresetId> =
            if (isAudio) listOf(VOICE_AUDIO, GENERAL_AUDIO, HIGH_QUALITY_AUDIO, CUSTOM_AUDIO)
            else listOf(DISCREET_VIDEO, STANDARD_VIDEO, CUSTOM)

        fun getPresetDefinition(id: RecordingPresetId): PresetDefinition = when (id) {
            DISCREET_VIDEO -> PresetDefinition(
                recordingType = "VIDEO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK).copy(name = "Pure Black", brightnessPercent = 1)
            )
            STANDARD_VIDEO -> PresetDefinition(
                recordingType = "VIDEO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.CLOCK).copy(name = "OLED Clock", brightnessPercent = 30)
            )
            VOICE_AUDIO -> PresetDefinition(
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK).copy(name = "Pure Black", brightnessPercent = 1),
                audioQuality = "STANDARD",
                audioClearVoice = true,
                audioReduceEcho = false,
                audioChannels = 1
            )
            GENERAL_AUDIO -> PresetDefinition(
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK).copy(name = "Pure Black", brightnessPercent = 1),
                audioQuality = "STANDARD",
                audioClearVoice = false,
                audioReduceEcho = false,
                audioChannels = 1
            )
            HIGH_QUALITY_AUDIO -> PresetDefinition(
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK).copy(name = "Pure Black", brightnessPercent = 1),
                audioQuality = "MAXIMUM",
                audioClearVoice = false,
                audioReduceEcho = false,
                audioChannels = 2
            )
            CUSTOM_AUDIO -> PresetDefinition(
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK)
            )
            AUDIO -> PresetDefinition(
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK).copy(name = "Pure Black", brightnessPercent = 1),
                audioQuality = "STANDARD",
                audioClearVoice = false,
                audioReduceEcho = false,
                audioChannels = 1
            )
            CUSTOM -> PresetDefinition(
                recordingType = "VIDEO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig.forPreset(CoverPresetId.PURE_BLACK)
            )
        }
    }
}

typealias RecordingPreset = RecordingPresetId

data class PresetDefinition(
    val recordingType: String,
    val videoQuality: String = "1080p",
    val useFrontCamera: Boolean = false,
    val coverConfig: CoverConfig = CoverConfig.PURE_BLACK_PRESET,
    val audioQuality: String = "HIGH",
    val audioClearVoice: Boolean = false,
    val audioReduceEcho: Boolean = false,
    val audioChannels: Int = 2
)

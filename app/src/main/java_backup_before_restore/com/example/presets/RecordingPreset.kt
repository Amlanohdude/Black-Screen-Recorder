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
                id = id,
                recordingType = "VIDEO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.PURE_BLACK, name = "Pure Black")
            )
            STANDARD_VIDEO -> PresetDefinition(
                id = id,
                recordingType = "VIDEO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.CLOCK, name = "Clock", showClock = true)
            )
            VOICE_AUDIO -> PresetDefinition(
                id = id,
                recordingType = "AUDIO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.PURE_BLACK, name = "Pure Black")
            )
            GENERAL_AUDIO -> PresetDefinition(
                id = id,
                recordingType = "AUDIO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.PURE_BLACK, name = "Pure Black")
            )
            HIGH_QUALITY_AUDIO -> PresetDefinition(
                id = id,
                recordingType = "AUDIO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.PURE_BLACK, name = "Pure Black")
            )
            CUSTOM_AUDIO -> PresetDefinition(
                id = id,
                recordingType = "AUDIO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.CUSTOM, name = "Custom")
            )
            AUDIO -> PresetDefinition(
                id = id,
                recordingType = "AUDIO",
                videoQuality = "720p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.PURE_BLACK, name = "Pure Black")
            )
            CUSTOM -> PresetDefinition(
                id = id,
                recordingType = "VIDEO",
                videoQuality = "1080p",
                useFrontCamera = false,
                coverConfig = CoverConfig(presetId = CoverPresetId.CUSTOM, name = "Custom")
            )
        }
    }
}

typealias RecordingPreset = RecordingPresetId

data class PresetDefinition(
    val id: RecordingPresetId,
    val recordingType: String,
    val videoQuality: String = "1080p",
    val useFrontCamera: Boolean = false,
    val coverConfig: CoverConfig
)

package com.example.recording

enum class AudioMicSource(
    val id: String,
    val displayName: String,
    val iconDescription: String
) {
    PHONE_MIC("phone_mic", "Built-in device microphone", "Built-in device microphone"),
    WIRED_HEADSET("wired_headset", "Connected 3.5mm or USB-C headset", "Connected 3.5mm or USB-C headset"),
    BLUETOOTH_MIC("bluetooth_mic", "Bluetooth microphone", "Connected Bluetooth headset or microphone");

    companion object {
        fun fromId(id: String): AudioMicSource {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PHONE_MIC
        }
    }
}

enum class AudioQuality(
    val id: String,
    val title: String,
    val description: String,
    val bitrate: Int,
    val sampleRate: Int
) {
    HIGH("high", "High (256 kbps)", "Best fidelity for music & studio clarity", 256000, 48000),
    STANDARD("standard", "Standard (128 kbps)", "Balanced size and vocal clarity", 128000, 44100),
    VOICE("voice", "Voice (64 kbps)", "Optimized for speech, lectures & small file size", 64000, 22050);

    companion object {
        fun fromId(id: String): AudioQuality {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STANDARD
        }
    }
}

data class AudioRecordingConfig(
    val microphone: AudioMicSource = AudioMicSource.PHONE_MIC,
    val quality: AudioQuality = AudioQuality.STANDARD,
    val reduceEcho: Boolean = false,
    val clearVoice: Boolean = false,
    val inputVolume: Float = 1.0f,
    val bitrate: Int = 128000,
    val sampleRate: Int = 44100,
    val format: String = "M4A",
    val channels: Int = 1
)

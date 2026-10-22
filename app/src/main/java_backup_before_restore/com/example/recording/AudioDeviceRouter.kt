package com.example.recording

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

class AudioRoutingSession(
    private val audioManager: AudioManager?,
    private val previousMode: Int,
    private val wasScoStarted: Boolean,
    private val routingActivated: Boolean
) {
    fun release() {
        try {
            if (audioManager != null) {
                if (wasScoStarted) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
                if (routingActivated) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        audioManager.clearCommunicationDevice()
                    }
                    audioManager.mode = previousMode
                }
            }
        } catch (e: Exception) {
            Log.e("AudioDeviceRouter", "Error releasing audio routing session", e)
        }
    }
}

object AudioDeviceRouter {
    private const val TAG = "AudioDeviceRouter"

    fun getAvailableMicrophones(context: Context): List<AudioMicSource> {
        val list = mutableListOf(AudioMicSource.PHONE_MIC)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return list

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        var hasWired = false
        var hasBt = false

        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> hasWired = true

                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET -> hasBt = true
            }
        }

        if (hasWired) list.add(AudioMicSource.WIRED_HEADSET)
        if (hasBt) list.add(AudioMicSource.BLUETOOTH_MIC)

        return list
    }

    fun routeAudioInput(
        context: Context,
        preferred: AudioMicSource,
        recorder: MediaRecorder? = null
    ): AudioRoutingSession {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AudioRoutingSession(null, 0, false, false)

        val previousMode = audioManager.mode
        var wasScoStarted = false
        var routingActivated = false

        try {
            val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val targetDevice = findMatchingDevice(inputDevices, preferred)

            if (targetDevice != null) {
                if (recorder != null) {
                    try {
                        val success = recorder.setPreferredDevice(targetDevice)
                        Log.d(TAG, "recorder.setPreferredDevice set to ${targetDevice.productName} ($success)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to setPreferredDevice on recorder", e)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    val success = audioManager.setCommunicationDevice(targetDevice)
                    Log.d(TAG, "audioManager.setCommunicationDevice result: $success")
                    routingActivated = true
                } else if (preferred == AudioMicSource.BLUETOOTH_MIC) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                    wasScoStarted = true
                    routingActivated = true
                }
            } else {
                Log.w(TAG, "Preferred microphone $preferred not physically connected; falling back to default Phone microphone")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error routing audio device", e)
        }

        return AudioRoutingSession(audioManager, previousMode, wasScoStarted, routingActivated)
    }

    private fun findMatchingDevice(
        devices: Array<AudioDeviceInfo>,
        preferred: AudioMicSource
    ): AudioDeviceInfo? {
        return when (preferred) {
            AudioMicSource.PHONE_MIC -> {
                devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    ?: devices.firstOrNull()
            }
            AudioMicSource.WIRED_HEADSET -> {
                devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
            }
            AudioMicSource.BLUETOOTH_MIC -> {
                devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
            }
        }
    }
}

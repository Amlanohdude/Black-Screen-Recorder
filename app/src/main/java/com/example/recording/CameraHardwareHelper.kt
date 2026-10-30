package com.example.recording

import android.content.Context
import android.media.CamcorderProfile
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recorder

object CameraHardwareHelper {
    private const val TAG = "CameraHardwareHelper"

    /**
     * Testing override to simulate 4K sensor support on devices/emulators lacking 4K hardware.
     */
    var force4kSupportForTesting: Boolean? = null

    /**
     * Checks if the specified camera (front vs back) supports 4K UHD (2160p) video recording.
     * Uses CameraX VideoCapabilities with CamcorderProfile fallback.
     */
    fun is4KSupported(context: Context, useFrontCamera: Boolean): Boolean {
        if (force4kSupportForTesting != null) {
            return force4kSupportForTesting!!
        }
        return try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            if (provider.hasCamera(cameraSelector)) {
                val cameraInfo = provider.getCameraInfo(cameraSelector)
                val videoCaps = Recorder.getVideoCapabilities(cameraInfo)
                val supported = videoCaps.isQualitySupported(Quality.UHD, DynamicRange.SDR)
                Log.d(TAG, "Camera 4K support (front=$useFrontCamera): $supported")
                supported
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "ProcessCameraProvider query failed, using CamcorderProfile fallback: ${e.message}")
            val cameraId = if (useFrontCamera) 1 else 0
            try {
                CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_2160P)
            } catch (_: Exception) {
                false
            }
        }
    }
}

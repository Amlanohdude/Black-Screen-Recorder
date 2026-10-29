package com.example.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class AdScreen {
    COVERS,
    RECORDINGS,
    RECORDINGS_AUDIO,
    SETTINGS,
    PLAYBACK
}

object NativeAdManager {
    private const val TAG = "NativeAdManager"
    private val isInitialized = java.util.concurrent.atomic.AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(context.applicationContext) {
                    android.util.Log.d(TAG, "Google Mobile Ads initialized successfully.")
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to initialize MobileAds: ${e.message}")
            }
        }
    }

    private val _coversAd = MutableStateFlow<NativeAd?>(null)
    private val _recordingsAd = MutableStateFlow<NativeAd?>(null)
    private val _recordingsAudioAd = MutableStateFlow<NativeAd?>(null)
    private val _settingsAd = MutableStateFlow<NativeAd?>(null)
    private val _playbackAd = MutableStateFlow<NativeAd?>(null)

    val coversAd: StateFlow<NativeAd?> = _coversAd.asStateFlow()
    val recordingsAd: StateFlow<NativeAd?> = _recordingsAd.asStateFlow()
    val recordingsAudioAd: StateFlow<NativeAd?> = _recordingsAudioAd.asStateFlow()
    val settingsAd: StateFlow<NativeAd?> = _settingsAd.asStateFlow()
    val playbackAd: StateFlow<NativeAd?> = _playbackAd.asStateFlow()

    private val _coversAdLoading = MutableStateFlow(false)
    private val _recordingsAdLoading = MutableStateFlow(false)
    private val _recordingsAudioAdLoading = MutableStateFlow(false)
    private val _settingsAdLoading = MutableStateFlow(false)
    private val _playbackAdLoading = MutableStateFlow(false)

    val coversAdLoading: StateFlow<Boolean> = _coversAdLoading.asStateFlow()
    val recordingsAdLoading: StateFlow<Boolean> = _recordingsAdLoading.asStateFlow()
    val recordingsAudioAdLoading: StateFlow<Boolean> = _recordingsAudioAdLoading.asStateFlow()
    val settingsAdLoading: StateFlow<Boolean> = _settingsAdLoading.asStateFlow()
    val playbackAdLoading: StateFlow<Boolean> = _playbackAdLoading.asStateFlow()

    private var isRecordingActive = false

    fun getAdFlow(screen: AdScreen): StateFlow<NativeAd?> {
        return when (screen) {
            AdScreen.COVERS -> _coversAd.asStateFlow()
            AdScreen.RECORDINGS -> _recordingsAd.asStateFlow()
            AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAd.asStateFlow()
            AdScreen.SETTINGS -> _settingsAd.asStateFlow()
            AdScreen.PLAYBACK -> _playbackAd.asStateFlow()
        }
    }

    fun isLoadingFlow(screen: AdScreen): StateFlow<Boolean> {
        return when (screen) {
            AdScreen.COVERS -> _coversAdLoading.asStateFlow()
            AdScreen.RECORDINGS -> _recordingsAdLoading.asStateFlow()
            AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAdLoading.asStateFlow()
            AdScreen.SETTINGS -> _settingsAdLoading.asStateFlow()
            AdScreen.PLAYBACK -> _playbackAdLoading.asStateFlow()
        }
    }

    fun onRecordingStarted() {
        isRecordingActive = true
    }

    fun onRecordingStopped() {
        isRecordingActive = false
    }

    fun loadAdForScreen(
        context: Context,
        screen: AdScreen,
        preferVideo: Boolean = (screen == AdScreen.COVERS || screen == AdScreen.RECORDINGS || screen == AdScreen.PLAYBACK),
        isFallback: Boolean = false
    ) {
        if (!AdsConfig.isAdsEnabled()) return
        if (isRecordingActive) return

        val isScreenEnabled = when (screen) {
            AdScreen.COVERS -> AdsConfig.showInCovers
            AdScreen.RECORDINGS -> AdsConfig.showInRecordings
            AdScreen.RECORDINGS_AUDIO -> AdsConfig.showInRecordingsAudio
            AdScreen.SETTINGS -> AdsConfig.showInSettings
            AdScreen.PLAYBACK -> true
        }
        if (!isScreenEnabled) return

        val currentAd = when (screen) {
            AdScreen.COVERS -> _coversAd.value
            AdScreen.RECORDINGS -> _recordingsAd.value
            AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAd.value
            AdScreen.SETTINGS -> _settingsAd.value
            AdScreen.PLAYBACK -> _playbackAd.value
        }
        if (currentAd != null && !isFallback) return

        val isLoading = when (screen) {
            AdScreen.COVERS -> _coversAdLoading.value
            AdScreen.RECORDINGS -> _recordingsAdLoading.value
            AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAdLoading.value
            AdScreen.SETTINGS -> _settingsAdLoading.value
            AdScreen.PLAYBACK -> _playbackAdLoading.value
        }
        if (isLoading && !isFallback) return

        initialize(context)
        setLoading(screen, true)
        Log.d(TAG, "Loading native ad for $screen (preferVideo=$preferVideo, isFallback=$isFallback)")

        val adUnitId = AdsConfig.getNativeAdUnitId(preferVideo)
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()
        val nativeAdOptions = NativeAdOptions.Builder()
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
            .setVideoOptions(videoOptions)
            .build()

        try {
            val adLoader = AdLoader.Builder(context.applicationContext, adUnitId)
                .forNativeAd { nativeAd ->
                    when (screen) {
                        AdScreen.COVERS -> _coversAd.value = nativeAd
                        AdScreen.RECORDINGS -> _recordingsAd.value = nativeAd
                        AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAd.value = nativeAd
                        AdScreen.SETTINGS -> _settingsAd.value = nativeAd
                        AdScreen.PLAYBACK -> _playbackAd.value = nativeAd
                    }
                    setLoading(screen, false)
                    Log.d(TAG, "Successfully loaded native ad for $screen (preferVideo=$preferVideo)")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Failed to load native ad for $screen (preferVideo=$preferVideo, isFallback=$isFallback): [${loadAdError.code}] ${loadAdError.message}")
                        if (preferVideo && !isFallback) {
                            Log.i(TAG, "Video inventory unavailable for $screen (code: ${loadAdError.code}). Triggering fallback to static native ad unit...")
                            loadAdForScreen(context, screen, preferVideo = false, isFallback = true)
                        } else {
                            setLoading(screen, false)
                        }
                    }
                })
                .withNativeAdOptions(nativeAdOptions)
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.w(TAG, "Error initiating ad load for $screen: ${e.message}")
            setLoading(screen, false)
        }
    }

    fun preloadInitialAds(context: Context) {
        if (!AdsConfig.isAdsEnabled() || isRecordingActive) return
        initialize(context)
        if (AdsConfig.showInCovers && _coversAd.value == null) {
            loadAdForScreen(context, AdScreen.COVERS, preferVideo = true)
        }
        if (AdsConfig.showInRecordings && _recordingsAd.value == null) {
            loadAdForScreen(context, AdScreen.RECORDINGS, preferVideo = true)
        }
    }

    fun destroyAd(screen: AdScreen) {
        when (screen) {
            AdScreen.COVERS -> {
                _coversAd.value?.destroy()
                _coversAd.value = null
            }
            AdScreen.RECORDINGS -> {
                _recordingsAd.value?.destroy()
                _recordingsAd.value = null
            }
            AdScreen.RECORDINGS_AUDIO -> {
                _recordingsAudioAd.value?.destroy()
                _recordingsAudioAd.value = null
            }
            AdScreen.SETTINGS -> {
                _settingsAd.value?.destroy()
                _settingsAd.value = null
            }
            AdScreen.PLAYBACK -> {
                _playbackAd.value?.destroy()
                _playbackAd.value = null
            }
        }
    }

    fun destroyAll() {
        destroyAd(AdScreen.COVERS)
        destroyAd(AdScreen.RECORDINGS)
        destroyAd(AdScreen.RECORDINGS_AUDIO)
        destroyAd(AdScreen.SETTINGS)
        destroyAd(AdScreen.PLAYBACK)
    }

    private fun setLoading(screen: AdScreen, loading: Boolean) {
        when (screen) {
            AdScreen.COVERS -> _coversAdLoading.value = loading
            AdScreen.RECORDINGS -> _recordingsAdLoading.value = loading
            AdScreen.RECORDINGS_AUDIO -> _recordingsAudioAdLoading.value = loading
            AdScreen.SETTINGS -> _settingsAdLoading.value = loading
            AdScreen.PLAYBACK -> _playbackAdLoading.value = loading
        }
    }
}

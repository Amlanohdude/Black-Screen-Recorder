package com.example.ads

import com.example.BuildConfig
import com.example.storage.EntitlementManager

object AdsConfig {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115"
    const val TEST_NATIVE_STATIC_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val PROD_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-6996252057357821/4208354575"
    private const val PROD_NATIVE_STATIC_AD_UNIT_ID = "ca-app-pub-6996252057357821/4208354575"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6996252057357821/3183494626"

    const val showInCovers: Boolean = true
    const val coversFrequency: Int = 4

    const val showInRecordings: Boolean = true
    const val recordingsFrequency: Int = 4

    const val showInRecordingsAudio: Boolean = true
    const val recordingsAudioFrequency: Int = 4

    const val showInSettings: Boolean = true
    const val showInPlayback: Boolean = true

    const val showInRecordTab: Boolean = false
    const val showInActiveRecording: Boolean = false
    const val showOnLockScreen: Boolean = false

    fun isAdsEnabled(): Boolean = !EntitlementManager.isPro()
    fun isTestMode(): Boolean = BuildConfig.DEBUG

    fun getNativeAdUnitId(preferVideo: Boolean = true): String {
        return if (isTestMode()) {
            if (preferVideo) TEST_NATIVE_VIDEO_AD_UNIT_ID else TEST_NATIVE_STATIC_AD_UNIT_ID
        } else {
            if (preferVideo) PROD_NATIVE_VIDEO_AD_UNIT_ID else PROD_NATIVE_STATIC_AD_UNIT_ID
        }
    }

    fun getRewardedAdUnitId(): String = if (isTestMode()) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
}

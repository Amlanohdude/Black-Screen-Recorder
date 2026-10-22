package com.example.ads

object AdsConfig {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_NATIVE_STATIC_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val PROD_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val PROD_NATIVE_STATIC_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

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

    fun isAdsEnabled(): Boolean = true
    fun isTestMode(): Boolean = true

    fun getNativeAdUnitId(preferVideo: Boolean = false): String {
        return if (preferVideo) TEST_NATIVE_VIDEO_AD_UNIT_ID else TEST_NATIVE_STATIC_AD_UNIT_ID
    }

    fun getRewardedAdUnitId(): String = TEST_REWARDED_AD_UNIT_ID
}

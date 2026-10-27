package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    private var rewardedAd: RewardedAd? = null
    private var isRecordingActive = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    fun onRecordingStarted() {
        isRecordingActive = true
        rewardedAd = null
        _isAdReady.value = false
        _isLoading.value = false
    }

    fun onRecordingStopped() {
        isRecordingActive = false
    }

    fun loadAd(context: Context) {
        if (isRecordingActive) {
            if (AdsConfig.isTestMode()) {
                Log.d(TAG, "Skipping rewarded ad load: active recording in progress.")
            }
            return
        }
        if (rewardedAd != null || _isLoading.value) {
            return
        }
        NativeAdManager.initialize(context)
        _isLoading.value = true
        val adUnitId = AdsConfig.getRewardedAdUnitId()
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (isRecordingActive) {
                        rewardedAd = null
                        _isAdReady.value = false
                        _isLoading.value = false
                        return
                    }
                    if (AdsConfig.isTestMode()) {
                        Log.d(TAG, "Rewarded ad loaded successfully.")
                    }
                    rewardedAd = ad
                    _isAdReady.value = true
                    _isLoading.value = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: [${loadAdError.code}] ${loadAdError.message}")
                    rewardedAd = null
                    _isAdReady.value = false
                    _isLoading.value = false
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onAdDismissed: () -> Unit = {},
        onFailedToShow: (String) -> Unit = {}
    ) {
        if (isRecordingActive) {
            onFailedToShow("Cannot display ads while a recording session is active.")
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            onFailedToShow("Rewarded ad isn't available right now. Please try again later.")
            loadAd(activity.applicationContext)
            return
        }

        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                if (AdsConfig.isTestMode()) {
                    Log.d(TAG, "Rewarded ad presented full-screen.")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdReady.value = false
                onFailedToShow(adError.message)
                loadAd(activity.applicationContext)
            }

            override fun onAdDismissedFullScreenContent() {
                if (AdsConfig.isTestMode()) {
                    Log.d(TAG, "Rewarded ad dismissed. Reward granted: $rewardGranted")
                }
                rewardedAd = null
                _isAdReady.value = false
                onAdDismissed()
                loadAd(activity.applicationContext)
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            if (AdsConfig.isTestMode()) {
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
            onUserEarnedReward(rewardItem)
        }
    }
}

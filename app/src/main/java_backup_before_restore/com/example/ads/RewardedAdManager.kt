package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    private var rewardedAd: RewardedAd? = null

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var isRecordingActive = false

    fun onRecordingStarted() {
        isRecordingActive = true
    }

    fun onRecordingStopped() {
        isRecordingActive = false
    }

    fun loadAd(context: Context) {
        if (!AdsConfig.isAdsEnabled()) return
        if (isRecordingActive) return
        if (_isLoading.value || rewardedAd != null) return

        _isLoading.value = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdsConfig.getRewardedAdUnitId()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isAdReady.value = true
                    _isLoading.value = false
                    Log.d(TAG, "RewardedAd successfully loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isAdReady.value = false
                    _isLoading.value = false
                    Log.w(TAG, "RewardedAd failed to load: ${error.message}")
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onFailedToShow: () -> Unit = {},
        onAdDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onFailedToShow()
            return
        }

        ad.show(activity) {
            onUserEarnedReward()
        }

        rewardedAd = null
        _isAdReady.value = false
        onAdDismissed()
        loadAd(activity)
    }
}

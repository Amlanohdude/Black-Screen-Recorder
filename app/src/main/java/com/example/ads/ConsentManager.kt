package com.example.ads

import android.app.Activity
import android.util.Log
import com.example.BuildConfig
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private const val TAG = "ConsentManager"

    fun gatherConsent(activity: Activity, onConsentGathered: (Boolean) -> Unit) {
        // Amazon Fire OS and non-play distributions do not support Google Play Services UMP
        if (BuildConfig.FLAVOR != "play") {
            Log.d(TAG, "Flavor is ${BuildConfig.FLAVOR}; bypassing UserMessagingPlatform.")
            onConsentGathered(true)
            return
        }

        try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
            consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            if (formError != null) {
                                Log.w(TAG, "Consent form error: ${formError.errorCode}: ${formError.message}")
                            }
                            val canRequest = try {
                                consentInfo.canRequestAds()
                            } catch (t: Throwable) {
                                true
                            }
                            onConsentGathered(canRequest)
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "Error displaying consent form: ${t.message}", t)
                        onConsentGathered(true)
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "Consent info request failed: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                    val canRequest = try {
                        consentInfo.canRequestAds()
                    } catch (t: Throwable) {
                        true
                    }
                    onConsentGathered(canRequest)
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "UserMessagingPlatform not supported or failed on this device: ${t.message}", t)
            onConsentGathered(true)
        }
    }

    fun canRequestAds(activity: Activity): Boolean {
        if (BuildConfig.FLAVOR != "play") return true
        return try {
            UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
        } catch (t: Throwable) {
            Log.w(TAG, "canRequestAds check failed: ${t.message}")
            true
        }
    }
}

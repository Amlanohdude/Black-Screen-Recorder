package com.example.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

object ConsentManager {
    private const val TAG = "ConsentManager"
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    fun gatherConsent(activity: Activity, onConsentGathered: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode}: ${formError.message}")
                    }
                    onConsentGathered(consentInfo.canRequestAds())
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent info request failed: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                onConsentGathered(consentInfo.canRequestAds())
            }
        )
    }

    fun canRequestAds(activity: Activity): Boolean {
        return UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
    }
}

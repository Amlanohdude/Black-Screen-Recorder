package com.example.config

/**
 * Centralized links and configuration for external feedback, feature requests, privacy policy, terms, and app sharing.
 */
object AppLinks {
    /**
     * Google Form or external URL for user bug reports and general feedback.
     */
    const val FEEDBACK_BUG_REPORT_FORM_URL: String =
        "https://docs.google.com/forms/d/e/1FAIpQLSfiiUet-T22ltZx1DlH9q5vM6gmC1_SVd0BpeZOs5nXiFF_RA/viewform?usp=pp_url"

    var FEEDBACK_FORM_URL: String = FEEDBACK_BUG_REPORT_FORM_URL

    /**
     * Google Form or external URL for new feature requests.
     */
    const val FEATURE_REQUEST_FORM_URL: String =
        "https://docs.google.com/forms/d/e/1FAIpQLScC_mlGORtY2-9yPHDJOrPVKKmGmj7M5ykKpAd7zNZ6Azo--A/viewform?usp=pp_url"

    private const val ENTRY_BUG_DEVICE_MODEL = "entry.42842753"
    private const val ENTRY_BUG_ANDROID_VERSION = "entry.2074858404"
    private const val ENTRY_BUG_APP_VERSION = "entry.1626753253"
    private const val ENTRY_FEATURE_APP_VERSION = "entry.1916654104"

    fun getBugReportUrl(appVersion: String = "1.0.0"): String {
        val deviceModel = android.net.Uri.encode("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        val osVersion = android.net.Uri.encode("Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        val encodedAppVersion = android.net.Uri.encode(appVersion)
        return "$FEEDBACK_BUG_REPORT_FORM_URL&$ENTRY_BUG_DEVICE_MODEL=$deviceModel&$ENTRY_BUG_ANDROID_VERSION=$osVersion&$ENTRY_BUG_APP_VERSION=$encodedAppVersion"
    }

    fun getFeatureRequestUrl(appVersion: String = "1.0.0"): String {
        val encodedAppVersion = android.net.Uri.encode(appVersion)
        return "$FEATURE_REQUEST_FORM_URL&$ENTRY_FEATURE_APP_VERSION=$encodedAppVersion"
    }

    /**
     * Official Privacy Policy URL hosted on Notion.
     */
    const val PRIVACY_POLICY_URL: String =
        "https://blackscreenrecorder.notion.site/Privacy-Policy-for-Black-Screen-Recorder-3f099be3feef8012bab1e805df2ed5b0"

    /**
     * Official Terms and Conditions URL hosted on Notion.
     */
    const val TERMS_AND_CONDITIONS_URL: String =
        "https://blackscreenrecorder.notion.site/Terms-and-Conditions-for-Black-Screen-Recorder-3f099be3feef80998108f6dc086c67f5"

    /**
     * Google Play Store listing URL for sharing the app.
     */
    const val PLAY_STORE_URL: String =
        "https://play.google.com/store/apps/details?id=com.blackscreenrecorder"

    /**
     * Default text used when sharing the app.
     */
    const val SHARE_APP_TEXT: String =
        "Check out Black Screen Recorder - Discreet, secure, and privacy-focused screen recording:\n$PLAY_STORE_URL"
}

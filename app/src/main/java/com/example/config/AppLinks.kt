package com.example.config

/**
 * Centralized links and configuration for external feedback, feature requests, privacy policy, terms, and app sharing.
 */
object AppLinks {
    /**
     * Google Form or external URL for user bug reports and general feedback.
     */
    var FEEDBACK_FORM_URL: String = ""

    /**
     * Google Form or external URL for new feature requests.
     */
    var FEATURE_REQUEST_FORM_URL: String = ""

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

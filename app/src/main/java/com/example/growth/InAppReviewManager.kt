package com.example.growth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.ShareCompat
import com.example.config.AppLinks
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manages organic rating, Google Play In-App Reviews, and viral app sharing.
 *
 * Implements 100% Google Play Policy-compliant organic review velocity:
 * - Triggers at natural happy milestone (e.g. 3 successful recordings).
 * - Uses official Google Play In-App Review API (no review gating or pre-screening questions).
 * - Safe fallback to direct Play Store intent when invoked manually from Settings.
 */
object InAppReviewManager {

    private const val TAG = "InAppReviewManager"
    private const val PREFS_NAME = "bsr_growth_prefs"
    private const val KEY_RECORDING_SUCCESS_COUNT = "successful_recording_count"
    private const val KEY_HAS_PROMPTED_REVIEW = "has_prompted_review_milestone"
    const val MILESTONE_TARGET_RECORDINGS = 3

    /**
     * Called whenever a recording session finishes and is saved successfully.
     * When reaching the 3rd successful recording milestone, launches the official In-App Review sheet.
     */
    fun onRecordingSavedSuccessfully(activity: Activity?) {
        if (activity == null) return
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_RECORDING_SUCCESS_COUNT, 0) + 1
        prefs.edit().putInt(KEY_RECORDING_SUCCESS_COUNT, currentCount).apply()

        val alreadyPrompted = prefs.getBoolean(KEY_HAS_PROMPTED_REVIEW, false)
        Log.d(TAG, "Recording saved. Total success count: $currentCount, alreadyPrompted: $alreadyPrompted")

        if (!alreadyPrompted && currentCount >= MILESTONE_TARGET_RECORDINGS) {
            launchInAppReview(activity) {
                prefs.edit().putBoolean(KEY_HAS_PROMPTED_REVIEW, true).apply()
            }
        }
    }

    /**
     * Launches the official Google Play In-App Review sheet.
     * Google handles the presentation, quota management, and submission.
     */
    fun launchInAppReview(activity: Activity, onComplete: () -> Unit = {}) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        Log.d(TAG, "In-app review flow completed.")
                        onComplete()
                    }
                } else {
                    Log.w(TAG, "In-app review request failed: ${task.exception?.message}")
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception launching in-app review", e)
            onComplete()
        }
    }

    /**
     * Directly opens the app's Google Play Store listing for manual review/rating.
     * Used by the "Rate on Google Play" button in Settings.
     */
    fun openPlayStorePage(context: Context) {
        val packageName = context.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(goToMarket)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PLAY_STORE_URL)).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Triggers the Android ShareSheet with pre-formatted app recommendation text.
     */
    fun shareApp(context: Context) {
        try {
            val activity = context as? Activity
            val shareBuilder = ShareCompat.IntentBuilder(activity ?: context)
                .setType("text/plain")
                .setText(AppLinks.SHARE_APP_TEXT)
                .setSubject("Black Screen Recorder for Android")
                .setChooserTitle("Share Black Screen Recorder")

            shareBuilder.intent.putExtra(Intent.EXTRA_TITLE, "Black Screen Recorder")
            val chooserIntent = shareBuilder.createChooserIntent().apply {
                putExtra(Intent.EXTRA_TITLE, "Black Screen Recorder")
                if (activity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Error opening share sheet", e)
        }
    }

    // ==========================================
    // DEVELOPER QA & TESTING HELPERS
    // ==========================================

    fun getRecordingSuccessCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_RECORDING_SUCCESS_COUNT, 0)
    }

    fun hasPromptedReview(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_PROMPTED_REVIEW, false)
    }

    fun resetReviewMilestone(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_RECORDING_SUCCESS_COUNT)
            .remove(KEY_HAS_PROMPTED_REVIEW)
            .apply()
    }
}

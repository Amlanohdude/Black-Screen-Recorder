package com.example.growth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppReviewManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        InAppReviewManager.resetReviewMilestone(context)
    }

    @Test
    fun milestoneTarget_isThree() {
        assertEquals(3, InAppReviewManager.MILESTONE_TARGET_RECORDINGS)
    }

    @Test
    fun initialRecordingSuccessCount_isZero() {
        assertEquals(0, InAppReviewManager.getRecordingSuccessCount(context))
        assertFalse(InAppReviewManager.hasPromptedReview(context))
    }

    @Test
    fun resetReviewMilestone_clearsCountAndPromptedFlag() {
        val prefs = context.getSharedPreferences("bsr_growth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("successful_recording_count", 5)
            .putBoolean("has_prompted_review_milestone", true)
            .apply()

        assertEquals(5, InAppReviewManager.getRecordingSuccessCount(context))
        assertTrue(InAppReviewManager.hasPromptedReview(context))

        InAppReviewManager.resetReviewMilestone(context)

        assertEquals(0, InAppReviewManager.getRecordingSuccessCount(context))
        assertFalse(InAppReviewManager.hasPromptedReview(context))
    }
}

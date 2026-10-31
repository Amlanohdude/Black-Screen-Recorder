package com.example.billing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PremiumManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val sp = context.getSharedPreferences("bsr_premium_entitlements", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
        val entSp = context.getSharedPreferences("bsr_entitlements", Context.MODE_PRIVATE)
        entSp.edit().clear().commit()
        PremiumManager.resetForTesting()
    }

    @Test
    fun initialState_isNotPro() {
        PremiumManager.init(context)
        assertFalse(PremiumManager.isPro())
        assertFalse(PremiumManager.isPro.value)
    }

    @Test
    fun setPro_updatesStateAndPersists() {
        PremiumManager.init(context)
        PremiumManager.setPro(true)
        assertTrue(PremiumManager.isPro())
        assertTrue(PremiumManager.isPro.value)

        // Verify direct persistence in SharedPreferences
        val sp = context.getSharedPreferences("bsr_premium_entitlements", Context.MODE_PRIVATE)
        assertTrue(sp.getBoolean("is_pro_purchased", false))
    }

    @Test
    fun setProFalse_revokesEntitlement() {
        PremiumManager.init(context)
        PremiumManager.setPro(true)
        assertTrue(PremiumManager.isPro())

        PremiumManager.setPro(false)
        assertFalse(PremiumManager.isPro())
        assertFalse(PremiumManager.isPro.value)
    }
}

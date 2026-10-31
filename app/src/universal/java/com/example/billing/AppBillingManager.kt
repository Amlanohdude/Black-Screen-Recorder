package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.razorpay.Checkout
import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Razorpay Checkout implementation of AppBillingManager.
 * Strictly isolated to the 'universal' product flavor for direct website/APK downloads.
 * Supports UPI (GPay, PhonePe, Paytm), NetBanking, and Cards.
 */
object AppBillingManager {
    private const val TAG = "AppBillingManager[Universal]"
    const val PRO_PRODUCT_ID = BillingConstants.PRO_PRODUCT_ID

    // Replace with live key for production release or via build config / environment
    var RAZORPAY_KEY_ID: String = "rzp_test_placeholder"

    private var appContext: Context? = null
    private val _uiState = MutableStateFlow(
        BillingUiState(
            productAvailable = true,
            formattedPrice = "₹99.00",
            standardFormattedPrice = "₹199.00",
            originalFormattedPrice = "₹199.00",
            discountPercentage = 50
        )
    )
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        PremiumManager.init(app)
        _uiState.value = _uiState.value.copy(
            isPro = PremiumManager.isPro()
        )

        try {
            Checkout.preload(app)
        } catch (e: Exception) {
            Log.w(TAG, "Razorpay preload warning: ${e.message}")
        }
    }

    fun onResume() {
        _uiState.value = _uiState.value.copy(isPro = PremiumManager.isPro())
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        onConnected?.invoke()
    }

    fun queryProductDetails() {
        // Universal direct pricing is statically initialized with promotional pricing
        _uiState.value = _uiState.value.copy(
            productAvailable = true,
            formattedPrice = "₹99.00",
            standardFormattedPrice = "₹199.00",
            originalFormattedPrice = "₹199.00",
            discountPercentage = 50
        )
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String = PRO_PRODUCT_ID): Boolean {
        _uiState.value = _uiState.value.copy(isPurchasePending = true)
        RazorpayCheckoutActivity.start(activity, skuId, amountPaise = 9900L, currency = "INR")
        return true
    }

    fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        Log.d(TAG, "Payment succeeded: paymentId=$paymentId, orderId=${data?.orderId}")
        PremiumManager.setPro(true)
        _uiState.value = _uiState.value.copy(
            isPro = true,
            isPurchasePending = false,
            userMessage = "PRO Lifetime Activated!"
        )
    }

    fun onPaymentError(errorCode: Int, message: String?) {
        Log.w(TAG, "Payment error/cancelled: code=$errorCode, msg=$message")
        _uiState.value = _uiState.value.copy(
            isPurchasePending = false,
            userMessage = if (errorCode != 0) "Payment could not be completed." else null
        )
    }

    fun restorePurchases(activity: Activity, onComplete: (Boolean) -> Unit) {
        restorePurchases { success, _ -> onComplete(success) }
    }

    fun restorePurchases(onFeedback: (Boolean, String) -> Unit) {
        val pro = PremiumManager.isPro()
        _uiState.value = _uiState.value.copy(isPro = pro)
        if (pro) {
            onFeedback(true, "PRO Lifetime Restored Successfully!")
        } else {
            onFeedback(false, "No previous purchases found on this device.")
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun formatPriceFromMicros(formattedSample: String, targetMicros: Long, currencyCode: String?): String? =
        PriceFormatter.formatPriceFromMicros(formattedSample, targetMicros, currencyCode)

    fun calculateOriginalPrice(
        formattedPrice: String,
        amountMicros: Long,
        currencyCode: String?,
        discountPercentage: Int = 50
    ): String? = PriceFormatter.calculateOriginalPrice(formattedPrice, amountMicros, currencyCode, discountPercentage)
}

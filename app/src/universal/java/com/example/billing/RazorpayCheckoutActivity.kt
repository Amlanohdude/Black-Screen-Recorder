package com.example.billing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

/**
 * Translucent activity handling Razorpay Checkout UI and callbacks.
 * Isolates Razorpay dependencies to the 'universal' flavor without polluting src/main.
 */
class RazorpayCheckoutActivity : Activity(), PaymentResultWithDataListener {

    companion object {
        private const val TAG = "RazorpayActivity"
        const val EXTRA_SKU_ID = "extra_sku_id"
        const val EXTRA_AMOUNT_PAISE = "extra_amount_paise"
        const val EXTRA_CURRENCY = "extra_currency"

        fun start(activity: Activity, skuId: String, amountPaise: Long = 9900L, currency: String = "INR") {
            val intent = Intent(activity, RazorpayCheckoutActivity::class.java).apply {
                putExtra(EXTRA_SKU_ID, skuId)
                putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
                putExtra(EXTRA_CURRENCY, currency)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val skuId = intent.getStringExtra(EXTRA_SKU_ID) ?: BillingConstants.PRO_PRODUCT_ID
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 9900L)
        val currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "INR"

        launchRazorpay(skuId, amountPaise, currency)
    }

    private fun launchRazorpay(skuId: String, amountPaise: Long, currency: String) {
        val checkout = Checkout()
        checkout.setKeyID(AppBillingManager.RAZORPAY_KEY_ID)

        try {
            val options = JSONObject().apply {
                put("name", "Black Screen Recorder PRO")
                put("description", "Lifetime PRO Access (All Features + Ad-Free)")
                put("currency", currency)
                put("amount", amountPaise)
                put("theme.color", "#E5A93C") // AMOLED Gold accent

                val retry = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retry)

                val prefill = JSONObject().apply {
                    put("email", "support@blackscreenrecorder.com")
                }
                put("prefill", prefill)

                val notes = JSONObject().apply {
                    put("sku_id", skuId)
                    put("product", "pro_lifetime")
                }
                put("notes", notes)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Razorpay Checkout", e)
            AppBillingManager.onPaymentError(-1, e.message ?: "Failed to open checkout")
            finish()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Log.d(TAG, "Payment Success: $razorpayPaymentId")
        AppBillingManager.onPaymentSuccess(razorpayPaymentId, paymentData)
        finish()
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        Log.w(TAG, "Payment Failed/Cancelled: code=$errorCode, response=$response")
        AppBillingManager.onPaymentError(errorCode, response)
        finish()
    }
}

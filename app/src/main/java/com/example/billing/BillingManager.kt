package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.storage.EntitlementManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingUiState(
    val isConnecting: Boolean = false,
    val isPro: Boolean = false,
    val productAvailable: Boolean = false,
    val productDetails: ProductDetails? = null,
    val formattedPrice: String? = null,
    val standardFormattedPrice: String? = null,
    val offerAvailable: Boolean = false,
    val discountedFormattedPrice: String? = null,
    val originalFormattedPrice: String? = null,
    val discountPercentage: Int? = null,
    val selectedOfferToken: String? = null,
    val isPurchasePending: Boolean = false,
    val isRestoring: Boolean = false,
    val userMessage: String? = null
)

object BillingManager : PurchasesUpdatedListener {
    private const val TAG = "BillingManager"
    const val PRO_PRODUCT_ID = "black_screen_recorder_pro_lifetime"
    private const val MAX_RECONNECT_ATTEMPTS = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var billingClient: BillingClient? = null
    private var appContext: Context? = null
    private var isConnecting = false
    private var reconnectAttempts = 0

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        _uiState.value = _uiState.value.copy(
            isPro = EntitlementManager.entitlements.value.isPro
        )

        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(app)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()

        startConnection()
    }

    fun onResume() {
        if (billingClient?.isReady == true) {
            queryPurchases()
        } else {
            startConnection()
        }
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        val client = billingClient ?: return
        if (client.isReady) {
            onConnected?.invoke()
            return
        }

        if (isConnecting) return
        isConnecting = true
        _uiState.value = _uiState.value.copy(isConnecting = true)

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                _uiState.value = _uiState.value.copy(isConnecting = false)

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    Log.d(TAG, "BillingClient connected successfully.")
                    queryProductDetails()
                    queryPurchases()
                    onConnected?.invoke()
                } else {
                    Log.w(TAG, "Billing setup failed: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    handleConnectionFailure()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                _uiState.value = _uiState.value.copy(isConnecting = false)
                handleConnectionFailure()
            }
        })
    }

    private fun handleConnectionFailure() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Log.d(TAG, "Retrying billing connection (attempt $reconnectAttempts)...")
        }
    }

    fun calculateOriginalPrice(
        formattedPrice: String,
        amountMicros: Long,
        currencyCode: String?,
        discountPercentage: Int = 50
    ): String? {
        if (formattedPrice.isBlank() || amountMicros <= 0L || discountPercentage <= 0 || discountPercentage >= 100) {
            return null
        }

        val factor = 100.0 / (100.0 - discountPercentage)
        val originalAmount = (amountMicros.toDouble() * factor) / 1_000_000.0

        // 1. Try regex replacement in formattedPrice to preserve exact currency symbol and position
        val pattern = java.util.regex.Pattern.compile("([0-9][0-9.,\\s]*[0-9]|[0-9]+)")
        val matcher = pattern.matcher(formattedPrice)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.trim() ?: ""
            val hasTwoDecimalsComma = numStr.matches(Regex(""".*,[0-9]{2}$"""))
            val hasTwoDecimalsDot = numStr.matches(Regex(""".*\.[0-9]{2}$"""))

            val replacement = when {
                hasTwoDecimalsComma -> String.format(java.util.Locale.US, "%.2f", originalAmount).replace('.', ',')
                hasTwoDecimalsDot -> String.format(java.util.Locale.US, "%.2f", originalAmount)
                originalAmount % 1.0 != 0.0 -> String.format(java.util.Locale.US, "%.2f", originalAmount)
                else -> String.format(java.util.Locale.US, "%.0f", originalAmount)
            }
            return matcher.replaceFirst(replacement)
        }

        // 2. Fallback to NumberFormat with currencyCode
        if (!currencyCode.isNullOrEmpty()) {
            try {
                val currency = java.util.Currency.getInstance(currencyCode)
                val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).apply {
                    this.currency = currency
                    maximumFractionDigits = 2
                    minimumFractionDigits = if (originalAmount % 1.0 == 0.0) 0 else 2
                }
                return format.format(originalAmount)
            } catch (_: Exception) {}
        }

        return null
    }

    fun queryProductDetails() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryProductDetailsResult.productDetailsList.firstOrNull { it.productId == PRO_PRODUCT_ID }
                if (details != null) {
                    val offer = details.oneTimePurchaseOfferDetails
                    val formattedPrice = offer?.formattedPrice
                    val amountMicros = offer?.priceAmountMicros ?: 0L
                    val currencyCode = offer?.priceCurrencyCode
                    val discountPercentage = 50
                    val originalPrice = if (formattedPrice != null && amountMicros > 0L) {
                        calculateOriginalPrice(formattedPrice, amountMicros, currencyCode, discountPercentage)
                    } else null

                    _uiState.value = _uiState.value.copy(
                        productAvailable = true,
                        productDetails = details,
                        formattedPrice = formattedPrice,
                        standardFormattedPrice = originalPrice ?: formattedPrice,
                        originalFormattedPrice = originalPrice,
                        discountPercentage = if (originalPrice != null) discountPercentage else null
                    )
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity): Boolean {
        val client = billingClient ?: return false
        val details = _uiState.value.productDetails ?: return false

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = client.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(isPurchasePending = true)
            return true
        }
        return false
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _uiState.value = _uiState.value.copy(isPurchasePending = false)
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled by user.")
        } else {
            Log.w(TAG, "Purchase error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRO_PRODUCT_ID)) {
                EntitlementManager.grantPro(true)
                _uiState.value = _uiState.value.copy(
                    isPro = true,
                    userMessage = "PRO Lifetime Activated!"
                )
            }
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(params) { result ->
                    Log.d(TAG, "Purchase acknowledged: ${result.responseCode}")
                }
            }
        }
    }

    fun queryPurchases(onResult: ((Boolean) -> Unit)? = null) {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            var hasPro = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.products.contains(PRO_PRODUCT_ID)) {
                        hasPro = true
                        handlePurchase(purchase)
                    }
                }
            }
            if (hasPro) {
                EntitlementManager.grantPro(true)
            }
            _uiState.value = _uiState.value.copy(isPro = hasPro || EntitlementManager.entitlements.value.isPro)
            onResult?.invoke(hasPro)
        }
    }

    fun restorePurchases(onFeedback: (Boolean, String) -> Unit) {
        _uiState.value = _uiState.value.copy(isRestoring = true)
        startConnection {
            queryPurchases { hasPro ->
                _uiState.value = _uiState.value.copy(isRestoring = false)
                if (hasPro) {
                    onFeedback(true, "PRO Lifetime Restored Successfully!")
                } else {
                    onFeedback(false, "No active PRO purchase found on this Google account.")
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}

package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Store Billing implementation of AppBillingManager.
 * Strictly isolated to the 'play' product flavor.
 */
object AppBillingManager : PurchasesUpdatedListener {
    private const val TAG = "AppBillingManager[Play]"
    const val PRO_PRODUCT_ID = BillingConstants.PRO_PRODUCT_ID
    private const val MAX_RECONNECT_ATTEMPTS = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var billingClient: BillingClient? = null
    private var appContext: Context? = null
    private var isConnecting = false
    private var reconnectAttempts = 0
    private var cachedProductDetails: ProductDetails? = null

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        PremiumManager.init(app)
        _uiState.value = _uiState.value.copy(
            isPro = PremiumManager.isPro()
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
                    cachedProductDetails = details
                    val defaultOffer = details.oneTimePurchaseOfferDetails
                    val allOffers = details.oneTimePurchaseOfferDetailsList

                    val offer = allOffers?.minByOrNull { it.priceAmountMicros } ?: defaultOffer
                    val formattedPrice = offer?.formattedPrice
                    val amountMicros = offer?.priceAmountMicros ?: 0L
                    val currencyCode = offer?.priceCurrencyCode
                    val fullPriceMicros = offer?.fullPriceMicros
                    val offerToken = offer?.offerToken

                    val baseOffer = allOffers?.maxByOrNull { it.priceAmountMicros }
                    val baseFormattedPrice = baseOffer?.formattedPrice

                    val discountPercentage = offer?.discountDisplayInfo?.percentageDiscount ?: 50
                    val originalPrice = when {
                        !baseFormattedPrice.isNullOrEmpty() && baseFormattedPrice != formattedPrice -> baseFormattedPrice
                        fullPriceMicros != null && fullPriceMicros > amountMicros ->
                            PriceFormatter.formatPriceFromMicros(formattedPrice ?: "", fullPriceMicros, currencyCode)
                        formattedPrice != null && amountMicros > 0L ->
                            PriceFormatter.calculateOriginalPrice(formattedPrice, amountMicros, currencyCode, discountPercentage)
                        else -> null
                    }

                    _uiState.value = _uiState.value.copy(
                        productAvailable = true,
                        formattedPrice = formattedPrice,
                        standardFormattedPrice = originalPrice ?: formattedPrice,
                        originalFormattedPrice = originalPrice,
                        discountPercentage = if (originalPrice != null) discountPercentage else null,
                        selectedOfferToken = offerToken
                    )
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String = PRO_PRODUCT_ID): Boolean {
        val client = billingClient ?: return false
        val details = cachedProductDetails ?: return false
        val token = _uiState.value.selectedOfferToken

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                if (!token.isNullOrEmpty()) {
                    setOfferToken(token)
                }
            }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
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
                PremiumManager.setPro(true)
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
                PremiumManager.setPro(true)
            }
            _uiState.value = _uiState.value.copy(isPro = hasPro || PremiumManager.isPro())
            onResult?.invoke(hasPro)
        }
    }

    fun restorePurchases(activity: Activity, onComplete: (Boolean) -> Unit) {
        restorePurchases { success, _ -> onComplete(success) }
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

    fun formatPriceFromMicros(formattedSample: String, targetMicros: Long, currencyCode: String?): String? =
        PriceFormatter.formatPriceFromMicros(formattedSample, targetMicros, currencyCode)

    fun calculateOriginalPrice(
        formattedPrice: String,
        amountMicros: Long,
        currencyCode: String?,
        discountPercentage: Int = 50
    ): String? = PriceFormatter.calculateOriginalPrice(formattedPrice, amountMicros, currencyCode, discountPercentage)
}

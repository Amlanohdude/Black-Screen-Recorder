package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Amazon Appstore SDK implementation of AppBillingManager.
 * Strictly isolated to the 'amazon' product flavor.
 */
object AppBillingManager : PurchasingListener {
    private const val TAG = "AppBillingManager[Amazon]"
    const val PRO_PRODUCT_ID = BillingConstants.PRO_PRODUCT_ID

    private var appContext: Context? = null
    private var pendingRestoreCallback: ((Boolean, String) -> Unit)? = null

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        try {
            PremiumManager.init(app)
            _uiState.value = _uiState.value.copy(
                isPro = PremiumManager.isPro()
            )

            // Register listener with the passed context (Activity if available, else ApplicationContext)
            PurchasingService.registerListener(context, this)
            PurchasingService.enablePendingPurchases()

            PurchasingService.getUserData()
            PurchasingService.getPurchaseUpdates(false)
            queryProductDetails()
        } catch (t: Throwable) {
            Log.e(TAG, "Amazon IAP initialization failed: ${t.message}", t)
        }
    }

    fun onResume() {
        try {
            PurchasingService.getUserData()
            PurchasingService.getPurchaseUpdates(false)
        } catch (t: Throwable) {
            Log.w(TAG, "Amazon onResume query failed: ${t.message}")
        }
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        try {
            PurchasingService.getUserData()
        } catch (t: Throwable) {
            Log.w(TAG, "startConnection failed: ${t.message}")
        }
        onConnected?.invoke()
    }

    fun queryProductDetails() {
        try {
            val skus = setOf(PRO_PRODUCT_ID)
            PurchasingService.getProductData(skus)
        } catch (t: Throwable) {
            Log.w(TAG, "queryProductDetails failed: ${t.message}")
        }
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String = PRO_PRODUCT_ID): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isPurchasePending = true, userMessage = null)
            // Re-register with foreground Activity context to ensure modal attaches properly
            PurchasingService.registerListener(activity, this)
            val requestId = PurchasingService.purchase(skuId)
            Log.d(TAG, "Initiated Amazon purchase for $skuId, requestId=$requestId")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "launchPurchaseFlow failed: ${t.message}", t)
            _uiState.value = _uiState.value.copy(
                isPurchasePending = false,
                userMessage = "Failed to launch Amazon Appstore purchase. Please try again."
            )
            false
        }
    }

    fun restorePurchases(activity: Activity, onComplete: (Boolean) -> Unit) {
        restorePurchases { success, _ -> onComplete(success) }
    }

    fun restorePurchases(onFeedback: (Boolean, String) -> Unit) {
        try {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            pendingRestoreCallback = onFeedback
            PurchasingService.getPurchaseUpdates(true)
        } catch (t: Throwable) {
            Log.e(TAG, "restorePurchases failed: ${t.message}", t)
            _uiState.value = _uiState.value.copy(isRestoring = false)
            onFeedback(false, "Restore purchases failed.")
        }
    }

    // --- Amazon PurchasingListener Callbacks ---

    override fun onUserDataResponse(response: UserDataResponse) {
        Log.d(TAG, "onUserDataResponse: status=${response.requestStatus}")
        if (response.requestStatus == UserDataResponse.RequestStatus.SUCCESSFUL) {
            val userData = response.userData
            Log.d(TAG, "Amazon User: ${userData.userId}, marketplace=${userData.marketplace}")
        }
    }

    override fun onProductDataResponse(response: ProductDataResponse) {
        Log.d(TAG, "onProductDataResponse: status=${response.requestStatus}")
        if (response.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL) {
            val product = response.productData[PRO_PRODUCT_ID]
            if (product != null) {
                val formattedPrice = product.price
                _uiState.value = _uiState.value.copy(
                    productAvailable = true,
                    formattedPrice = formattedPrice,
                    standardFormattedPrice = formattedPrice
                )
            }
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        _uiState.value = _uiState.value.copy(isPurchasePending = false)
        Log.d(TAG, "onPurchaseResponse: status=${response.requestStatus}")
        when (response.requestStatus) {
            PurchaseResponse.RequestStatus.SUCCESSFUL -> {
                val receipt = response.receipt
                if (receipt.sku == PRO_PRODUCT_ID) {
                    PremiumManager.setPro(true)
                    PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
                    _uiState.value = _uiState.value.copy(
                        isPro = true,
                        userMessage = "PRO Lifetime Activated!"
                    )
                }
            }
            PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> {
                PremiumManager.setPro(true)
                _uiState.value = _uiState.value.copy(
                    isPro = true,
                    userMessage = "PRO Lifetime Restored!"
                )
            }
            PurchaseResponse.RequestStatus.FAILED -> {
                Log.w(TAG, "Amazon purchase failed: ${response.requestStatus}")
                _uiState.value = _uiState.value.copy(
                    userMessage = "Purchase could not be completed. Please try again."
                )
            }
            PurchaseResponse.RequestStatus.INVALID_SKU -> {
                Log.w(TAG, "Amazon purchase failed: ${response.requestStatus}")
                _uiState.value = _uiState.value.copy(
                    userMessage = "Item temporarily unavailable in Amazon Appstore. Please check back shortly."
                )
            }
            PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                Log.w(TAG, "Amazon purchase failed: ${response.requestStatus}")
                _uiState.value = _uiState.value.copy(
                    userMessage = "In-App Purchases are not supported on this device/configuration."
                )
            }
            PurchaseResponse.RequestStatus.PENDING -> {
                _uiState.value = _uiState.value.copy(isPurchasePending = true)
            }
            else -> {}
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        _uiState.value = _uiState.value.copy(isRestoring = false)
        Log.d(TAG, "onPurchaseUpdatesResponse: status=${response.requestStatus}")
        var hasPro = false
        if (response.requestStatus == PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            for (receipt in response.receipts) {
                if (receipt.sku == PRO_PRODUCT_ID && !receipt.isCanceled) {
                    hasPro = true
                    PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
                    break
                }
            }
        }
        if (hasPro) {
            PremiumManager.setPro(true)
        }
        _uiState.value = _uiState.value.copy(isPro = hasPro || PremiumManager.isPro())

        pendingRestoreCallback?.let { callback ->
            if (hasPro) {
                callback(true, "PRO Lifetime Restored Successfully!")
            } else {
                callback(false, "No active PRO purchase found on this Amazon account.")
            }
            pendingRestoreCallback = null
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

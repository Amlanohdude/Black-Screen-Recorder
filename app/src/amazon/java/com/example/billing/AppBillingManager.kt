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

        PremiumManager.init(app)
        _uiState.value = _uiState.value.copy(
            isPro = PremiumManager.isPro()
        )

        PurchasingService.registerListener(app, this)
        PurchasingService.enablePendingPurchases()

        PurchasingService.getUserData()
        PurchasingService.getPurchaseUpdates(false)
        queryProductDetails()
    }

    fun onResume() {
        PurchasingService.getUserData()
        PurchasingService.getPurchaseUpdates(false)
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        PurchasingService.getUserData()
        onConnected?.invoke()
    }

    fun queryProductDetails() {
        val skus = setOf(PRO_PRODUCT_ID)
        PurchasingService.getProductData(skus)
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String = PRO_PRODUCT_ID): Boolean {
        _uiState.value = _uiState.value.copy(isPurchasePending = true)
        val requestId = PurchasingService.purchase(skuId)
        Log.d(TAG, "Initiated Amazon purchase for $skuId, requestId=$requestId")
        return true
    }

    fun restorePurchases(activity: Activity, onComplete: (Boolean) -> Unit) {
        restorePurchases { success, _ -> onComplete(success) }
    }

    fun restorePurchases(onFeedback: (Boolean, String) -> Unit) {
        _uiState.value = _uiState.value.copy(isRestoring = true)
        pendingRestoreCallback = onFeedback
        PurchasingService.getPurchaseUpdates(true)
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
            PurchaseResponse.RequestStatus.FAILED,
            PurchaseResponse.RequestStatus.INVALID_SKU,
            PurchaseResponse.RequestStatus.NOT_SUPPORTED -> {
                Log.w(TAG, "Amazon purchase failed: ${response.requestStatus}")
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

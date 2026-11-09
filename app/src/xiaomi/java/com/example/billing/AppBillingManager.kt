package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.iap.common.apiHelper.IBillingClient
import com.iap.common.apiHelper.IPurchase
import com.xiaomi.billingclient.api.AcknowledgePurchaseResponseListener
import com.xiaomi.billingclient.api.BillingClient
import com.xiaomi.billingclient.api.BillingClientStateListener
import com.xiaomi.billingclient.api.BillingFlowParams
import com.xiaomi.billingclient.api.BillingResult
import com.xiaomi.billingclient.api.Purchase
import com.xiaomi.billingclient.api.PurchasesResponseListener
import com.xiaomi.billingclient.api.PurchasesUpdatedListener
import com.xiaomi.billingclient.api.SkuDetails
import com.xiaomi.billingclient.api.SkuDetailsParams
import com.xiaomi.billingclient.api.SkuDetailsResponseListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Xiaomi GetApps Billing Client implementation of AppBillingManager.
 * Strictly isolated to the 'xiaomi' product flavor.
 */
object AppBillingManager : PurchasesUpdatedListener {
    private const val TAG = "AppBillingManager[Xiaomi]"
    const val PRO_PRODUCT_ID = BillingConstants.PRO_PRODUCT_ID

    private var billingClient: BillingClient? = null
    private var appContext: Context? = null
    private var cachedSkuDetails: SkuDetails? = null

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

        billingClient = BillingClient.newBuilder(app)
            .setListener(this)
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

        _uiState.value = _uiState.value.copy(isConnecting = true)
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                _uiState.value = _uiState.value.copy(isConnecting = false)
                if (billingResult.responseCode == IBillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Xiaomi Billing connected successfully.")
                    queryProductDetails()
                    queryPurchases()
                    onConnected?.invoke()
                } else {
                    Log.w(TAG, "Xiaomi billing setup failed: code=${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _uiState.value = _uiState.value.copy(isConnecting = false)
                Log.d(TAG, "Xiaomi Billing service disconnected.")
            }
        })
    }

    fun queryProductDetails() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = SkuDetailsParams.newBuilder()
            .setSkusList(listOf(PRO_PRODUCT_ID))
            .setType(IBillingClient.SkuType.INAPP)
            .build()

        client.querySkuDetailsAsync(params, object : SkuDetailsResponseListener {
            override fun onSkuDetailsResponse(billingResult: BillingResult, skuDetailsList: List<SkuDetails>?) {
                if (billingResult.responseCode == IBillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                    val skuDetails = skuDetailsList.firstOrNull { it.sku == PRO_PRODUCT_ID }
                    if (skuDetails != null) {
                        cachedSkuDetails = skuDetails
                        val formattedPrice = skuDetails.price
                        _uiState.value = _uiState.value.copy(
                            productAvailable = true,
                            formattedPrice = formattedPrice,
                            standardFormattedPrice = formattedPrice
                        )
                    }
                }
            }
        })
    }

    fun launchPurchaseFlow(activity: Activity, skuId: String = PRO_PRODUCT_ID): Boolean {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.w(TAG, "launchPurchaseFlow called but billingClient is not ready. Reconnecting...")
            _uiState.value = _uiState.value.copy(
                userMessage = "Connecting to Xiaomi GetApps store. Please try again in a moment."
            )
            startConnection {
                queryProductDetails()
            }
            return false
        }

        val skuDetails = cachedSkuDetails
        if (skuDetails == null) {
            Log.w(TAG, "launchPurchaseFlow called but cachedSkuDetails is null. Querying product details...")
            _uiState.value = _uiState.value.copy(
                userMessage = "Loading in-app item details from Xiaomi GetApps. Please try again in a moment."
            )
            queryProductDetails()
            return false
        }

        return try {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()

            val result = client.launchBillingFlow(activity, flowParams)
            if (result.responseCode == IBillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Xiaomi launchBillingFlow successful")
                _uiState.value = _uiState.value.copy(isPurchasePending = true, userMessage = null)
                true
            } else {
                Log.w(TAG, "Xiaomi launchBillingFlow returned error code: ${result.responseCode}")
                _uiState.value = _uiState.value.copy(
                    isPurchasePending = false,
                    userMessage = "Xiaomi billing returned response code: ${result.responseCode}"
                )
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "launchPurchaseFlow threw exception: ${t.message}", t)
            _uiState.value = _uiState.value.copy(
                isPurchasePending = false,
                userMessage = "Failed to launch Xiaomi GetApps purchase: ${t.message}"
            )
            false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _uiState.value = _uiState.value.copy(isPurchasePending = false)
        if (billingResult.responseCode == IBillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == IBillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Xiaomi purchase canceled by user.")
        } else {
            Log.w(TAG, "Xiaomi purchase error: ${billingResult.responseCode}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == IPurchase.PurchaseState.PURCHASED) {
            if (purchase.skus?.contains(PRO_PRODUCT_ID) == true) {
                PremiumManager.setPro(true)
                _uiState.value = _uiState.value.copy(
                    isPro = true,
                    userMessage = "PRO Lifetime Activated!"
                )
            }
            if (!purchase.isAcknowledged) {
                val token = purchase.purchaseToken
                if (!token.isNullOrEmpty()) {
                    billingClient?.acknowledgePurchase(token, object : AcknowledgePurchaseResponseListener {
                        override fun onAcknowledgePurchaseResponse(result: BillingResult) {
                            Log.d(TAG, "Xiaomi purchase acknowledged: ${result.responseCode}")
                        }
                    })
                }
            }
        }
    }

    fun queryPurchases(onResult: ((Boolean) -> Unit)? = null) {
        val client = billingClient ?: return
        if (!client.isReady) return

        client.queryPurchasesAsync(IBillingClient.SkuType.INAPP, object : PurchasesResponseListener {
            override fun onQueryPurchasesResponse(billingResult: BillingResult, purchasesList: List<Purchase>) {
                var hasPro = false
                if (billingResult.responseCode == IBillingClient.BillingResponseCode.OK) {
                    for (purchase in purchasesList) {
                        if (purchase.skus?.contains(PRO_PRODUCT_ID) == true && purchase.purchaseState == IPurchase.PurchaseState.PURCHASED) {
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
        })
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
                    onFeedback(false, "No active PRO purchase found on this Xiaomi account.")
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

package com.example.billing

/**
 * Common, store-agnostic UI state representing the current billing and entitlement status.
 * Contains zero store-specific SDK dependencies to guarantee strict flavor isolation.
 */
data class BillingUiState(
    val isConnecting: Boolean = false,
    val isPro: Boolean = false,
    val productAvailable: Boolean = false,
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

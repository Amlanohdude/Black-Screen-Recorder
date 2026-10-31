package com.example.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingManagerTest {

    @Test
    fun calculateOriginalPrice_inrPriceWithDecimals_calculatesFiftyPercentAnchor() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "₹200.00",
            amountMicros = 200_000_000L,
            currencyCode = "INR",
            discountPercentage = 50
        )
        assertEquals("₹400.00", original)
    }

    @Test
    fun calculateOriginalPrice_usdPriceWithDecimals_calculatesFiftyPercentAnchor() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "$2.99",
            amountMicros = 2_990_000L,
            currencyCode = "USD",
            discountPercentage = 50
        )
        assertEquals("$5.98", original)
    }

    @Test
    fun calculateOriginalPrice_eurPriceWithComma_preservesCommaSeparator() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "2,49 €",
            amountMicros = 2_490_000L,
            currencyCode = "EUR",
            discountPercentage = 50
        )
        assertEquals("4,98 €", original)
    }

    @Test
    fun calculateOriginalPrice_inrPriceWithoutDecimals_calculatesFiftyPercentAnchorWithoutDecimals() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "₹200",
            amountMicros = 200_000_000L,
            currencyCode = "INR",
            discountPercentage = 50
        )
        assertEquals("₹400", original)
    }

    @Test
    fun calculateOriginalPrice_invalidInputs_returnsNull() {
        assertNull(BillingManager.calculateOriginalPrice("", 200_000_000L, "INR", 50))
        assertNull(BillingManager.calculateOriginalPrice("₹200.00", 0L, "INR", 50))
        assertNull(BillingManager.calculateOriginalPrice("₹200.00", 200_000_000L, "INR", 0))
        assertNull(BillingManager.calculateOriginalPrice("₹200.00", 200_000_000L, "INR", 100))
    }
}

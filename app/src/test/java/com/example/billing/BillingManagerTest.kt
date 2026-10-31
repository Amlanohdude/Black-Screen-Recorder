package com.example.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillingManagerTest {

    @Test
    fun calculateOriginalPrice_inrPriceWithDecimals_calculatesFiftyPercentAnchor() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "₹100.00",
            amountMicros = 100_000_000L,
            currencyCode = "INR",
            discountPercentage = 50
        )
        assertEquals("₹200.00", original)
    }

    @Test
    fun formatPriceFromMicros_formatsTargetMicrosDirectly() {
        val formatted = BillingManager.formatPriceFromMicros(
            formattedSample = "₹100.00",
            targetMicros = 200_000_000L,
            currencyCode = "INR"
        )
        assertEquals("₹200.00", formatted)
    }

    @Test
    fun formatPriceFromMicros_usdPsychologicalPricing_formatsBasePrice() {
        val formatted = BillingManager.formatPriceFromMicros(
            formattedSample = "$3.49",
            targetMicros = 6_990_000L,
            currencyCode = "USD"
        )
        assertEquals("$6.99", formatted)
    }

    @Test
    fun formatPriceFromMicros_gbpPsychologicalPricing_formatsBasePrice() {
        val formatted = BillingManager.formatPriceFromMicros(
            formattedSample = "£3.49",
            targetMicros = 6_990_000L,
            currencyCode = "GBP"
        )
        assertEquals("£6.99", formatted)
    }

    @Test
    fun calculateOriginalPrice_usdPriceWithDecimals_calculatesFiftyPercentAnchor() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "$3.50",
            amountMicros = 3_500_000L,
            currencyCode = "USD",
            discountPercentage = 50
        )
        assertEquals("$7.00", original)
    }

    @Test
    fun calculateOriginalPrice_eurPriceWithComma_preservesCommaSeparator() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "4,25 €",
            amountMicros = 4_250_000L,
            currencyCode = "EUR",
            discountPercentage = 50
        )
        assertEquals("8,50 €", original)
    }

    @Test
    fun calculateOriginalPrice_inrPriceWithoutDecimals_calculatesFiftyPercentAnchorWithoutDecimals() {
        val original = BillingManager.calculateOriginalPrice(
            formattedPrice = "₹100",
            amountMicros = 100_000_000L,
            currencyCode = "INR",
            discountPercentage = 50
        )
        assertEquals("₹200", original)
    }

    @Test
    fun calculateOriginalPrice_invalidInputs_returnsNull() {
        assertNull(BillingManager.calculateOriginalPrice("", 100_000_000L, "INR", 50))
        assertNull(BillingManager.calculateOriginalPrice("₹100.00", 0L, "INR", 50))
        assertNull(BillingManager.calculateOriginalPrice("₹100.00", 100_000_000L, "INR", 0))
        assertNull(BillingManager.calculateOriginalPrice("₹100.00", 100_000_000L, "INR", 100))
    }
}

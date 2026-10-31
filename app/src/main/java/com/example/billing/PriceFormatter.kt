package com.example.billing

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.regex.Pattern

/**
 * Pure price formatting and discount calculation utilities.
 * Completely decoupled from any vendor billing SDKs.
 */
object PriceFormatter {

    fun formatPriceFromMicros(
        formattedSample: String,
        targetMicros: Long,
        currencyCode: String?
    ): String? {
        if (formattedSample.isBlank() || targetMicros <= 0L) return null
        val targetAmount = targetMicros.toDouble() / 1_000_000.0

        val pattern = Pattern.compile("([0-9][0-9.,\\s]*[0-9]|[0-9]+)")
        val matcher = pattern.matcher(formattedSample)
        if (matcher.find()) {
            val numStr = matcher.group(1)?.trim() ?: ""
            val hasTwoDecimalsComma = numStr.matches(Regex(""".*,[0-9]{2}$"""))
            val hasTwoDecimalsDot = numStr.matches(Regex(""".*\.[0-9]{2}$"""))

            val replacement = when {
                hasTwoDecimalsComma -> String.format(Locale.US, "%.2f", targetAmount).replace('.', ',')
                hasTwoDecimalsDot -> String.format(Locale.US, "%.2f", targetAmount)
                targetAmount % 1.0 != 0.0 -> String.format(Locale.US, "%.2f", targetAmount)
                else -> String.format(Locale.US, "%.0f", targetAmount)
            }
            return matcher.replaceFirst(replacement)
        }

        if (!currencyCode.isNullOrEmpty()) {
            try {
                val currency = Currency.getInstance(currencyCode)
                val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                    this.currency = currency
                    maximumFractionDigits = 2
                    minimumFractionDigits = if (targetAmount % 1.0 == 0.0) 0 else 2
                }
                return format.format(targetAmount)
            } catch (_: Exception) {}
        }
        return null
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
        val originalAmountMicros = (amountMicros.toDouble() * factor).toLong()
        return formatPriceFromMicros(formattedPrice, originalAmountMicros, currencyCode)
    }
}

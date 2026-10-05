package com.qoody.shared.domain.model

/**
 * The form of a merchant name used to recognise the same merchant again: lower case, letters and
 * digits only, so "SWIGGY", "Swiggy" and "swiggy." match.
 */
object MerchantKey {
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{Nd}]")

    fun of(merchant: String): String = merchant.lowercase().replace(nonAlphanumeric, "")
}

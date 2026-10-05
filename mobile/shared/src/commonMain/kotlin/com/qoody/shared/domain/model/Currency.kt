package com.qoody.shared.domain.model

/** Currencies the ledger can be displayed in. Amounts are stored currency-less; see [Money]. */
enum class Currency(
    val symbol: String,
    val isoCode: String,
) {
    Usd(symbol = "$", isoCode = "USD"),
    Inr(symbol = "₹", isoCode = "INR"),
}

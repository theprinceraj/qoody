package com.qoody.shared.domain.format

import com.qoody.shared.domain.model.TransactionId

private const val RECEIPT_CODE_PREFIX = "LED-"
private const val RECEIPT_CODE_DIGITS = 4

/** The short human-friendly reference printed on a receipt, e.g. `LED-1042`. */
fun TransactionId.toReceiptCode(): String = RECEIPT_CODE_PREFIX + value.toString().padStart(RECEIPT_CODE_DIGITS, '0')

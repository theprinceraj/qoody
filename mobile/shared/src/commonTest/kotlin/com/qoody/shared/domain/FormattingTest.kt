package com.qoody.shared.domain

import com.qoody.shared.domain.format.LedgerCsv
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.MoneyInput
import com.qoody.shared.domain.format.PercentFormatter
import com.qoody.shared.domain.format.SignStyle
import com.qoody.shared.domain.format.toReceiptCode
import com.qoody.shared.domain.model.Money
import com.qoody.shared.domain.model.Permille
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.testZone
import com.qoody.shared.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyFormatterTest {
    @Test
    fun usesIndianGroupingAndAlwaysShowsTwoDecimals() {
        assertEquals("1,482.50", MoneyFormatter.formatPlain(Money.of(1482, 50)))
        assertEquals("12,34,567.89", MoneyFormatter.formatPlain(Money.of(1_234_567, 89)))
        assertEquals("1,00,00,000.00", MoneyFormatter.formatPlain(Money.of(10_000_000)))
        assertEquals("999.00", MoneyFormatter.formatPlain(Money.of(999)))
        assertEquals("0.00", MoneyFormatter.formatPlain(Money.Zero))
        assertEquals("4.05", MoneyFormatter.formatPlain(Money.of(4, 5)))
        assertEquals("₹1,483", MoneyFormatter.formatWhole(Money.of(1482, 50)))
        assertEquals("₹1,50,000", MoneyFormatter.formatWhole(Money.of(150_000)))
    }

    @Test
    fun addsRupeeSymbolAndOutflowSign() {
        assertEquals("₹4.50", MoneyFormatter.format(Money.of(4, 50)))
        assertEquals("-₹280.00", MoneyFormatter.format(Money.of(280), SignStyle.Outflow))
    }
}

class MoneyInputTest {
    @Test
    fun sanitizeKeepsDigitsOneSeparatorAndTwoDecimals() {
        assertEquals("12.34", MoneyInput.sanitize("12a.345"))
        assertEquals("1.23", MoneyInput.sanitize("1.2.3"))
        assertEquals("", MoneyInput.sanitize("abc"))
        assertEquals("123456789", MoneyInput.sanitize("1234567890123"))
    }

    @Test
    fun parseRejectsEmptyAndZeroAndReadsMinorUnits() {
        assertNull(MoneyInput.parse(""))
        assertNull(MoneyInput.parse("0"))
        assertNull(MoneyInput.parse("0.00"))
        assertEquals(Money.of(12, 50), MoneyInput.parse("12.5"))
        assertEquals(Money.of(0, 50), MoneyInput.parse(".5"))
        assertEquals(Money.of(7), MoneyInput.parse("7"))
    }
}

class PermilleTest {
    @Test
    fun roundsHalfUp() {
        assertEquals(333, Permille.of(Money.of(1), Money.of(3)).value)
        assertEquals(667, Permille.of(Money.of(2), Money.of(3)).value)
    }

    @Test
    fun zeroTotalGivesZero() {
        assertEquals(Permille.Zero, Permille.of(Money.of(5), Money.Zero))
    }

    @Test
    fun formatsWholeAndTenths() {
        assertEquals("36%", PercentFormatter.formatWhole(Permille(360)))
        assertEquals("100%", PercentFormatter.formatWhole(Permille(995)))
        assertEquals("99.4%", PercentFormatter.formatTenths(Permille(994)))
        assertEquals("2.4%", PercentFormatter.formatTenths(Permille(24)))
    }

    @Test
    fun capsAtFull() {
        assertEquals(Permille.Full, Permille(1_500).coerceAtMost(Permille.Full))
    }
}

class ReceiptCodeTest {
    @Test
    fun padsToFourDigits() {
        assertEquals("LED-0042", TransactionId(42).toReceiptCode())
        assertEquals("LED-8491", TransactionId(8491).toReceiptCode())
    }
}

class LedgerCsvTest {
    @Test
    fun writesHeaderAndEscapesSpecialCharacters() {
        val csv =
            LedgerCsv.build(
                listOf(transaction(daysAgo = 0, amount = Money.of(4, 50), merchant = "Cafe, \"Bar\"", note = "line")),
                testZone,
            )
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("id,date,time,merchant,amount,category,payment_app,tag,note", lines.first())
        assertEquals(2, lines.size)
        assertEquals(true, lines[1].contains("\"Cafe, \"\"Bar\"\"\""))
        assertEquals(true, lines[1].contains("4.50"))
    }
}

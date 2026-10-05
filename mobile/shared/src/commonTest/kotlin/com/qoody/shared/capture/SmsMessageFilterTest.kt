package com.qoody.shared.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmsMessageFilterTest {
    @Test
    fun acceptsHeadersAndBrandNamesWithBankWords() {
        assertTrue(SmsMessageFilter.mayBeBankAlert("JD-HDFCBK-S", "Sent Rs.5 from A/C *1"))
        assertTrue(SmsMessageFilter.mayBeBankAlert("HDFC Bank", "Rs 5 debited via UPI"))
        assertTrue(SmsMessageFilter.mayBeBankAlert("57575", "Card x1 used for Rs 5"))
    }

    @Test
    fun rejectsPhoneNumbersAndMessagesWithoutBankWords() {
        assertFalse(SmsMessageFilter.mayBeBankAlert("+91 98765 43210", "Rs 5 debited from A/c"))
        assertFalse(SmsMessageFilter.mayBeBankAlert("9876543210", "Rs 5 debited from A/c"))
        assertFalse(SmsMessageFilter.mayBeBankAlert("Rahul", "paid Rs 500 for dinner"))
    }

    @Test
    fun senderLabelStripsOperatorPrefixAndTypeSuffix() {
        assertEquals("HDFCBK", SmsMessageFilter.senderLabel("JD-HDFCBK-S"))
        assertEquals("SBIUPI", SmsMessageFilter.senderLabel("AD-SBIUPI"))
        assertEquals("HDFC Bank", SmsMessageFilter.senderLabel(" HDFC Bank "))
    }
}

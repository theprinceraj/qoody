package com.qoody.app

import com.qoody.shared.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SanityTest {
    @Test
    fun successStateHoldsGreeting() {
        assertEquals("Hi", HomeUiState.Success("Hi").greeting)
    }
}

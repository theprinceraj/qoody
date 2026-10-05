package com.qoody.shared.data

import com.qoody.shared.domain.repository.LlmKeyVerifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val SIMULATED_LATENCY_MILLIS = 800L

/** Placeholder verifier: accepts any non-blank key after a short delay. Replace with a real provider call. */
class FakeLlmKeyVerifier : LlmKeyVerifier {
    override suspend fun verify(key: String): Boolean {
        delay(SIMULATED_LATENCY_MILLIS.milliseconds)
        return key.isNotBlank()
    }
}

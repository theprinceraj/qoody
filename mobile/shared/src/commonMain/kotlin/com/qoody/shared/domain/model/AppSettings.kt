package com.qoody.shared.domain.model

enum class AppTheme {
    WarmPaper,
}

/** Configuration of the optional bring-your-own-key language model used to categorise entries. */
data class LlmSettings(
    val apiKey: String?,
    val modelLabel: String,
    val localFallbackReady: Boolean,
)

data class AppSettings(
    val onboardingCompleted: Boolean,
    val notificationListenerEnabled: Boolean,
    val monitoredAppCount: Int,
    val llm: LlmSettings,
    val currency: Currency,
    val theme: AppTheme,
    val hapticsEnabled: Boolean,
)

/** Outcome of checking an LLM API key. */
sealed interface KeyVerification {
    data object Idle : KeyVerification

    data object Testing : KeyVerification

    data object Verified : KeyVerification

    data object Failed : KeyVerification
}

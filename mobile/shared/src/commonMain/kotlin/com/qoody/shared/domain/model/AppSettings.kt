package com.qoody.shared.domain.model

enum class AppTheme {
    WarmPaper,
}

data class AppSettings(
    val onboardingCompleted: Boolean,
    val notificationListenerEnabled: Boolean,
    val monitoredAppCount: Int,
    val theme: AppTheme,
    val hapticsEnabled: Boolean,
)

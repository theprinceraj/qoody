package com.qoody.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.qoody.app.R
import kotlinx.serialization.Serializable

/**
 * Navigation keys. Top-level tabs implement [TabKey]; everything else is pushed on top of the current tab.
 * Keys are serializable so the back stack survives process death.
 */
sealed interface TabKey : NavKey

@Serializable
data class LedgerKey(
    val openSearch: Boolean = false,
) : TabKey

@Serializable
data object InsightsKey : TabKey

@Serializable
data object SettingsKey : TabKey

@Serializable
data class ReceiptKey(
    val transactionId: Long,
) : NavKey

/** The three entries of the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    Ledger(R.string.nav_ledger, R.drawable.ic_receipt_long, R.drawable.ic_receipt_long_filled),
    Insights(R.string.nav_insights, R.drawable.ic_bar_chart, R.drawable.ic_bar_chart_filled),
    Settings(R.string.nav_settings, R.drawable.ic_settings, R.drawable.ic_settings_filled),
    ;

    /** The key that shows this destination when it is first opened. */
    fun toKey(): TabKey =
        when (this) {
            Ledger -> LedgerKey()
            Insights -> InsightsKey
            Settings -> SettingsKey
        }

    companion object {
        fun of(key: TabKey): TopLevelDestination =
            when (key) {
                is LedgerKey -> Ledger
                InsightsKey -> Insights
                SettingsKey -> Settings
            }
    }
}

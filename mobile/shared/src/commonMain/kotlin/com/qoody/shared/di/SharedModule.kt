package com.qoody.shared.di

import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.FakeLlmKeyVerifier
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.LlmKeyVerifier
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.feature.insights.InsightsViewModel
import com.qoody.shared.feature.ledger.AddExpenseViewModel
import com.qoody.shared.feature.ledger.LedgerViewModel
import com.qoody.shared.feature.onboarding.OnboardingViewModel
import com.qoody.shared.feature.receipt.ReceiptViewModel
import com.qoody.shared.feature.root.RootViewModel
import com.qoody.shared.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Wiring for everything in `shared`. The in-memory repositories and the fake key verifier are
 * placeholders: swap these three bindings when real persistence and the LLM client exist.
 */
val sharedModule =
    module {
        single { DateProvider() }
        single<LedgerRepository> { InMemoryLedgerRepository(get()) }
        single<SettingsRepository> { InMemorySettingsRepository() }
        single<LlmKeyVerifier> { FakeLlmKeyVerifier() }

        viewModel { RootViewModel(get()) }
        viewModel { OnboardingViewModel(get()) }
        viewModel { LedgerViewModel(get(), get(), get()) }
        viewModel { AddExpenseViewModel(get(), get()) }
        viewModel { InsightsViewModel(get(), get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get()) }
        viewModel { (transactionId: Long) -> ReceiptViewModel(TransactionId(transactionId), get(), get(), get()) }
    }

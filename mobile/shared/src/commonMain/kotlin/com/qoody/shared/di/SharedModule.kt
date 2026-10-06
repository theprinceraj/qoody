package com.qoody.shared.di

import com.qoody.shared.capture.classifier.MerchantClassifier
import com.qoody.shared.core.DateProvider
import com.qoody.shared.data.InMemoryBudgetRepository
import com.qoody.shared.data.InMemoryLedgerRepository
import com.qoody.shared.data.InMemoryMerchantCategoryRepository
import com.qoody.shared.data.InMemorySettingsRepository
import com.qoody.shared.data.InMemoryUnparsedCaptureRepository
import com.qoody.shared.domain.model.TransactionId
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import com.qoody.shared.feature.budgets.BudgetsViewModel
import com.qoody.shared.feature.capture.UnparsedCapturesViewModel
import com.qoody.shared.feature.excluded.ExcludedEntriesViewModel
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
 * Wiring for everything in `shared`. The Android app overrides the in-memory repositories with Room
 * ones and [MerchantClassifier.None] with the bundled model.
 */
val sharedModule =
    module {
        single { DateProvider() }
        single<LedgerRepository> { InMemoryLedgerRepository(get()) }
        single<SettingsRepository> { InMemorySettingsRepository() }
        single<UnparsedCaptureRepository> { InMemoryUnparsedCaptureRepository() }
        single<MerchantCategoryRepository> { InMemoryMerchantCategoryRepository() }
        single<BudgetRepository> { InMemoryBudgetRepository() }
        single<MerchantClassifier> { MerchantClassifier.None }

        viewModel { RootViewModel(get()) }
        viewModel { OnboardingViewModel(get()) }
        viewModel { LedgerViewModel(get(), get(), get(), get()) }
        viewModel { AddExpenseViewModel(get(), get()) }
        viewModel { InsightsViewModel(get(), get()) }
        viewModel { SettingsViewModel(get(), get(), get(), get()) }
        viewModel { UnparsedCapturesViewModel(get(), get()) }
        viewModel { ExcludedEntriesViewModel(get(), get()) }
        viewModel { BudgetsViewModel(get(), get(), get()) }
        viewModel { (transactionId: Long) ->
            ReceiptViewModel(TransactionId(transactionId), get(), get(), get(), get(), get())
        }
    }

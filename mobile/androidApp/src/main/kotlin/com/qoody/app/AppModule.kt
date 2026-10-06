package com.qoody.app

import com.qoody.app.capture.AndroidNotificationAccessChecker
import com.qoody.app.capture.CaptureSources
import com.qoody.app.capture.NotificationAccessChecker
import com.qoody.app.capture.NotificationCaptureHandler
import com.qoody.app.data.BackupService
import com.qoody.app.data.QoodyDatabase
import com.qoody.app.data.RoomBudgetRepository
import com.qoody.app.data.RoomLedgerRepository
import com.qoody.app.data.RoomMerchantCategoryRepository
import com.qoody.app.data.RoomSettingsRepository
import com.qoody.app.data.RoomUnparsedCaptureRepository
import com.qoody.app.data.createQoodyDatabase
import com.qoody.shared.capture.CaptureNotificationUseCase
import com.qoody.shared.capture.classifier.LazyMerchantClassifier
import com.qoody.shared.capture.classifier.MerchantClassifier
import com.qoody.shared.domain.repository.BudgetRepository
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

fun appModule(database: QoodyDatabase) =
    module {
        single { database }
        single<LedgerRepository> { RoomLedgerRepository(get(), get()) }
        single<UnparsedCaptureRepository> { RoomUnparsedCaptureRepository(get()) }
        single<MerchantCategoryRepository> { RoomMerchantCategoryRepository(get()) }
        single<BudgetRepository> { RoomBudgetRepository(get()) }
        single<SettingsRepository> { RoomSettingsRepository(get()) }
        single<MerchantClassifier> {
            val assets = androidContext().assets
            LazyMerchantClassifier({ assets.open(MERCHANT_MODEL_ASSET).use { it.readBytes() } })
        }
        single { BackupService(get(), get()) }
        single {
            CaptureNotificationUseCase(
                ledger = get(),
                unparsed = get(),
                merchantCategories = get(),
                unknownMerchant = { androidContext().getString(R.string.capture_unknown_merchant) },
                classifier = get(),
                appKind = get<CaptureSources>()::kindOf,
            )
        }
        single { CaptureSources(androidContext()) }
        single { NotificationCaptureHandler(get(), androidContext().packageName, get()) }
        single<NotificationAccessChecker> { AndroidNotificationAccessChecker(androidContext()) }
    }

/** Trained by `tools/merchant-classifier`; see its README. */
private const val MERCHANT_MODEL_ASSET = "merchant_classifier.bin"

fun createAppModule(application: QoodyApplication) = appModule(createQoodyDatabase(application))

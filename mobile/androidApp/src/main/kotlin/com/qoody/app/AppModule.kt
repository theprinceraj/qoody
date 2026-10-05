package com.qoody.app

import com.qoody.app.capture.AndroidNotificationAccessChecker
import com.qoody.app.capture.CaptureSources
import com.qoody.app.capture.NotificationAccessChecker
import com.qoody.app.capture.NotificationCaptureHandler
import com.qoody.app.data.BackupService
import com.qoody.app.data.EncryptedKeyStore
import com.qoody.app.data.QoodyDatabase
import com.qoody.app.data.RoomLedgerRepository
import com.qoody.app.data.RoomMerchantCategoryRepository
import com.qoody.app.data.RoomSettingsRepository
import com.qoody.app.data.RoomUnparsedCaptureRepository
import com.qoody.app.data.createQoodyDatabase
import com.qoody.shared.capture.CaptureNotificationUseCase
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.MerchantCategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

fun appModule(database: QoodyDatabase) =
    module {
        single { database }
        single { EncryptedKeyStore(get()) }
        single<LedgerRepository> { RoomLedgerRepository(get(), get()) }
        single<UnparsedCaptureRepository> { RoomUnparsedCaptureRepository(get()) }
        single<MerchantCategoryRepository> { RoomMerchantCategoryRepository(get()) }
        single<SettingsRepository> { RoomSettingsRepository(get(), get()) }
        single { BackupService(get(), get()) }
        single {
            CaptureNotificationUseCase(
                ledger = get(),
                unparsed = get(),
                merchantCategories = get(),
                unknownMerchant = { androidContext().getString(R.string.capture_unknown_merchant) },
                appKind = get<CaptureSources>()::kindOf,
            )
        }
        single { CaptureSources(androidContext()) }
        single { NotificationCaptureHandler(get(), androidContext().packageName, get()) }
        single<NotificationAccessChecker> { AndroidNotificationAccessChecker(androidContext()) }
    }

fun createAppModule(application: QoodyApplication) = appModule(createQoodyDatabase(application))

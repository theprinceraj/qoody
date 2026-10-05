package com.qoody.app

import com.qoody.app.data.BackupService
import com.qoody.app.data.EncryptedKeyStore
import com.qoody.app.data.QoodyDatabase
import com.qoody.app.data.RoomLedgerRepository
import com.qoody.app.data.RoomSettingsRepository
import com.qoody.app.data.createQoodyDatabase
import com.qoody.shared.domain.repository.LedgerRepository
import com.qoody.shared.domain.repository.SettingsRepository
import org.koin.dsl.module

fun appModule(database: QoodyDatabase) =
    module {
        single { database }
        single { EncryptedKeyStore(get()) }
        single<LedgerRepository> { RoomLedgerRepository(get()) }
        single<SettingsRepository> { RoomSettingsRepository(get(), get()) }
        single { BackupService(get(), get()) }
    }

fun createAppModule(application: QoodyApplication) = appModule(createQoodyDatabase(application))

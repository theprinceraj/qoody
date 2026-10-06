package com.qoody.app

import android.app.Application
import com.qoody.app.data.LegacyApiKeyCleanup
import com.qoody.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QoodyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LegacyApiKeyCleanup.run(this)
        startKoin {
            androidContext(this@QoodyApplication)
            modules(sharedModule, createAppModule(this@QoodyApplication))
        }
    }
}

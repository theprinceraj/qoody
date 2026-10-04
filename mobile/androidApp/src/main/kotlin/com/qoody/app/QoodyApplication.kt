package com.qoody.app

import android.app.Application
import com.qoody.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QoodyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QoodyApplication)
            modules(sharedModule)
        }
    }
}

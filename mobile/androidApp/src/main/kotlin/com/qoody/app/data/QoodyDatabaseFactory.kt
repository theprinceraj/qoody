package com.qoody.app.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createQoodyDatabase(context: Context): QoodyDatabase =
    Room
        .databaseBuilder<QoodyDatabase>(
            context = context.applicationContext,
            name = context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath,
        ).setDriver(BundledSQLiteDriver())
        .build()

private const val DATABASE_NAME = "qoody.db"

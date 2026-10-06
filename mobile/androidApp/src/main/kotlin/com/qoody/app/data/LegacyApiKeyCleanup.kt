package com.qoody.app.data

import android.content.Context
import java.security.KeyStore

/**
 * Versions up to 0.4 could store a language-model API key, encrypted with an Android Keystore key.
 * Categorisation is now fully on-device, so a stored key is deleted along with its encryption key.
 */
object LegacyApiKeyCleanup {
    private const val PREFERENCES_NAME = "qoody_secure_preferences"
    private const val KEY_ALIAS = "qoody_api_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun run(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (preferences.all.isEmpty()) return
        preferences.edit().clear().commit()
        context.deleteSharedPreferences(PREFERENCES_NAME)
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }
}

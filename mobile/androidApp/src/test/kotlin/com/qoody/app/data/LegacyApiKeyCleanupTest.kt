package com.qoody.app.data

import android.app.Application
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class LegacyApiKeyCleanupTest {
    private val context get() = RuntimeEnvironment.getApplication()

    private fun legacyPreferences() = context.getSharedPreferences("qoody_secure_preferences", Context.MODE_PRIVATE)

    @Test
    fun deletesAKeyStoredByOlderVersions() {
        legacyPreferences().edit().putString("llm_api_key", "encrypted").commit()

        LegacyApiKeyCleanup.run(context)

        assertTrue(legacyPreferences().all.isEmpty())
    }

    @Test
    fun doesNothingWhenNoKeyWasStored() {
        LegacyApiKeyCleanup.run(context)

        assertTrue(legacyPreferences().all.isEmpty())
    }
}

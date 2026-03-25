package com.montheweb.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val defaultScrapeIntervalMinutes: Int = 10,
    val defaultTimeoutSeconds: Int = 5,
    val defaultExpectedHttpCode: Int = 200
)

class SettingsPreferences(private val context: Context) {

    private companion object {
        val KEY_SCRAPE_INTERVAL = intPreferencesKey("default_scrape_interval")
        val KEY_TIMEOUT = intPreferencesKey("default_timeout")
        val KEY_HTTP_CODE = intPreferencesKey("default_http_code")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultScrapeIntervalMinutes = prefs[KEY_SCRAPE_INTERVAL] ?: 10,
            defaultTimeoutSeconds = prefs[KEY_TIMEOUT] ?: 5,
            defaultExpectedHttpCode = prefs[KEY_HTTP_CODE] ?: 200
        )
    }

    suspend fun updateScrapeInterval(minutes: Int) {
        context.dataStore.edit { it[KEY_SCRAPE_INTERVAL] = minutes }
    }

    suspend fun updateTimeout(seconds: Int) {
        context.dataStore.edit { it[KEY_TIMEOUT] = seconds }
    }

    suspend fun updateExpectedHttpCode(code: Int) {
        context.dataStore.edit { it[KEY_HTTP_CODE] = code }
    }
}

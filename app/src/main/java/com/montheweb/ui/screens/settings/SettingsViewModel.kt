package com.montheweb.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.montheweb.MonTheWebApplication
import com.montheweb.data.preferences.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsPreferences = (application as MonTheWebApplication).settingsPreferences

    val settings: StateFlow<AppSettings> = settingsPreferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateScrapeInterval(minutes: String) {
        val value = minutes.toIntOrNull() ?: return
        if (value < 1) return
        viewModelScope.launch { settingsPreferences.updateScrapeInterval(value) }
    }

    fun updateTimeout(seconds: String) {
        val value = seconds.toIntOrNull() ?: return
        if (value < 1) return
        viewModelScope.launch { settingsPreferences.updateTimeout(value) }
    }

    fun updateExpectedHttpCode(code: String) {
        val value = code.toIntOrNull() ?: return
        if (value < 100 || value > 599) return
        viewModelScope.launch { settingsPreferences.updateExpectedHttpCode(value) }
    }
}

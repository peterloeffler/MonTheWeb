package com.montheweb.ui.screens.addurl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.montheweb.MonTheWebApplication
import com.montheweb.data.db.MonitoredUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AddEditUrlUiState(
    val name: String = "",
    val url: String = "",
    val scrapeIntervalMinutes: String = "",
    val timeoutSeconds: String = "",
    val expectedHttpCode: String = "",
    val expectedContent: String = "",
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: Boolean = false,
    val urlError: Boolean = false
)

class AddEditUrlViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as MonTheWebApplication
    private val repository = app.repository
    private val settingsPreferences = app.settingsPreferences

    private val urlId: Long = savedStateHandle.get<Long>("urlId") ?: -1L

    private val _uiState = MutableStateFlow(AddEditUrlUiState())
    val uiState: StateFlow<AddEditUrlUiState> = _uiState

    private var existingUrl: MonitoredUrl? = null

    init {
        viewModelScope.launch {
            if (urlId > 0) {
                // Editing existing URL
                val url = repository.getUrlById(urlId)
                if (url != null) {
                    existingUrl = url
                    _uiState.value = AddEditUrlUiState(
                        name = url.name,
                        url = url.url,
                        scrapeIntervalMinutes = url.scrapeIntervalMinutes.toString(),
                        timeoutSeconds = url.timeoutSeconds.toString(),
                        expectedHttpCode = url.expectedHttpCode.toString(),
                        expectedContent = url.expectedContent ?: "",
                        isEditing = true
                    )
                }
            } else {
                // New URL - load defaults from settings
                val settings = settingsPreferences.settingsFlow.first()
                _uiState.value = AddEditUrlUiState(
                    scrapeIntervalMinutes = settings.defaultScrapeIntervalMinutes.toString(),
                    timeoutSeconds = settings.defaultTimeoutSeconds.toString(),
                    expectedHttpCode = settings.defaultExpectedHttpCode.toString()
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = false)
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(url = url, urlError = false)
    }

    fun updateScrapeInterval(interval: String) {
        _uiState.value = _uiState.value.copy(scrapeIntervalMinutes = interval)
    }

    fun updateTimeout(timeout: String) {
        _uiState.value = _uiState.value.copy(timeoutSeconds = timeout)
    }

    fun updateExpectedHttpCode(code: String) {
        _uiState.value = _uiState.value.copy(expectedHttpCode = code)
    }

    fun updateExpectedContent(content: String) {
        _uiState.value = _uiState.value.copy(expectedContent = content)
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = true)
            hasError = true
        }
        if (state.url.isBlank()) {
            _uiState.value = _uiState.value.copy(urlError = true)
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            val interval = state.scrapeIntervalMinutes.toIntOrNull() ?: 10
            val timeout = state.timeoutSeconds.toIntOrNull() ?: 5
            val httpCode = state.expectedHttpCode.toIntOrNull() ?: 200
            val content = state.expectedContent.ifBlank { null }

            if (existingUrl != null) {
                repository.update(
                    existingUrl!!.copy(
                        name = state.name.trim(),
                        url = state.url.trim(),
                        scrapeIntervalMinutes = interval,
                        timeoutSeconds = timeout,
                        expectedHttpCode = httpCode,
                        expectedContent = content
                    )
                )
            } else {
                repository.insert(
                    MonitoredUrl(
                        name = state.name.trim(),
                        url = state.url.trim(),
                        scrapeIntervalMinutes = interval,
                        timeoutSeconds = timeout,
                        expectedHttpCode = httpCode,
                        expectedContent = content
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}

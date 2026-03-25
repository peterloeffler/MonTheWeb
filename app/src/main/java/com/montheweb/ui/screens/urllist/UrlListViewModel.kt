package com.montheweb.ui.screens.urllist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.montheweb.MonTheWebApplication
import com.montheweb.data.db.MonitoredUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UrlListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MonTheWebApplication).repository

    val urls: StateFlow<List<MonitoredUrl>> = repository.allUrlsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAllUrls()
            // Brief delay so the user sees the indicator
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun deleteUrl(url: MonitoredUrl) {
        viewModelScope.launch {
            repository.delete(url)
        }
    }

    fun reorderUrls(reorderedList: List<MonitoredUrl>) {
        viewModelScope.launch {
            repository.updateSortOrders(reorderedList)
        }
    }
}

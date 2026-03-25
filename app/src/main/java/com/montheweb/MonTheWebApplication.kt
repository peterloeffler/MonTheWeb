package com.montheweb

import android.app.Application
import com.montheweb.data.preferences.SettingsPreferences
import com.montheweb.data.repository.UrlRepository
import com.montheweb.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MonTheWebApplication : Application() {

    lateinit var repository: UrlRepository
        private set
    lateinit var settingsPreferences: SettingsPreferences
        private set
    lateinit var notificationHelper: NotificationHelper
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        settingsPreferences = SettingsPreferences(this)
        repository = UrlRepository(this)

        // Ensure all existing URL monitors are scheduled on app start
        applicationScope.launch {
            val urls = repository.getAllUrls()
            repository.scheduleAllUrlChecks(urls)
        }
    }
}

package com.montheweb.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.montheweb.data.db.AppDatabase
import com.montheweb.data.db.MonitoredUrl
import com.montheweb.data.db.MonitoredUrlDao
import com.montheweb.worker.UrlCheckWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class UrlRepository(context: Context) {

    private val dao: MonitoredUrlDao = AppDatabase.getInstance(context).monitoredUrlDao()
    private val workManager: WorkManager = WorkManager.getInstance(context)

    val allUrlsFlow: Flow<List<MonitoredUrl>> = dao.getAllUrlsFlow()

    suspend fun getAllUrls(): List<MonitoredUrl> = dao.getAllUrls()

    suspend fun getUrlById(id: Long): MonitoredUrl? = dao.getUrlById(id)

    suspend fun insert(url: MonitoredUrl): Long {
        val maxOrder = dao.getMaxSortOrder() ?: -1
        val newUrl = url.copy(sortOrder = maxOrder + 1)
        val id = dao.insert(newUrl)
        scheduleNextCheck(id, delayMinutes = 0)
        return id
    }

    suspend fun update(url: MonitoredUrl) {
        dao.update(url)
        scheduleNextCheck(url.id, delayMinutes = 0)
    }

    suspend fun delete(url: MonitoredUrl) {
        dao.delete(url)
        cancelUrlCheck(url.id)
    }

    suspend fun updateSortOrders(urls: List<MonitoredUrl>) {
        dao.updateAll(urls.mapIndexed { index, url -> url.copy(sortOrder = index) })
    }

    suspend fun updateCheckResult(id: Long, isAlerted: Boolean, lastError: String?) {
        dao.updateCheckResult(id, isAlerted, System.currentTimeMillis(), lastError)
    }

    /**
     * Schedule the next check for a URL.
     * Uses OneTimeWorkRequest with a delay for precise interval control.
     * A delay of 0 means run immediately (e.g. on add, edit, or manual refresh).
     */
    fun scheduleNextCheck(urlId: Long, delayMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(UrlCheckWorker.KEY_URL_ID to urlId)

        val workRequest = OneTimeWorkRequestBuilder<UrlCheckWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(workTagFor(urlId))
            .apply {
                if (delayMinutes > 0) {
                    setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                }
            }
            .build()

        workManager.enqueueUniqueWork(
            workNameFor(urlId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Trigger an immediate check for all URLs (used by pull-to-refresh).
     */
    suspend fun refreshAllUrls() {
        val urls = dao.getAllUrls()
        urls.forEach { scheduleNextCheck(it.id, delayMinutes = 0) }
    }

    /**
     * Schedule all URL checks on app start / boot.
     * Runs an immediate check for each URL; the worker self-reschedules
     * with the configured interval after each run.
     */
    fun scheduleAllUrlChecks(urls: List<MonitoredUrl>) {
        urls.forEach { url ->
            scheduleNextCheck(url.id, delayMinutes = 0)
        }
    }

    private fun cancelUrlCheck(urlId: Long) {
        workManager.cancelUniqueWork(workNameFor(urlId))
    }

    companion object {
        fun workNameFor(urlId: Long): String = "url_check_$urlId"
        fun workTagFor(urlId: Long): String = "url_check_tag_$urlId"
    }
}

package com.montheweb.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.montheweb.data.db.AppDatabase
import com.montheweb.data.db.MonitoredUrl
import com.montheweb.data.repository.UrlRepository
import com.montheweb.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class UrlCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL_ID = "url_id"
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 5_000L
    }

    /** Result of a single check attempt. */
    private sealed class CheckResult {
        data object Success : CheckResult()
        data class Failed(val error: String) : CheckResult()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urlId = inputData.getLong(KEY_URL_ID, -1)
        if (urlId == -1L) return@withContext Result.failure()

        val dao = AppDatabase.getInstance(applicationContext).monitoredUrlDao()
        val monitoredUrl = dao.getUrlById(urlId) ?: return@withContext Result.failure()
        val notificationHelper = NotificationHelper(applicationContext)

        dao.setChecking(urlId, true)

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(monitoredUrl.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout(monitoredUrl.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .writeTimeout(monitoredUrl.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            var lastError: String? = null

            for (attempt in 1..MAX_RETRIES) {
                when (val result = performCheck(client, monitoredUrl)) {
                    is CheckResult.Success -> {
                        markRecovered(dao, notificationHelper, urlId)
                        lastError = null
                        break
                    }
                    is CheckResult.Failed -> {
                        lastError = result.error
                        if (attempt < MAX_RETRIES) {
                            delay(RETRY_DELAY_MS)
                        }
                    }
                }
            }

            // Only alert if all retries failed
            if (lastError != null) {
                markAlert(dao, notificationHelper, urlId, monitoredUrl.name,
                    "$lastError (after $MAX_RETRIES attempts)")
            }
        } finally {
            dao.setChecking(urlId, false)
        }

        // Self-reschedule: queue the next check after the configured interval
        val repository = UrlRepository(applicationContext)
        repository.scheduleNextCheck(urlId, monitoredUrl.scrapeIntervalMinutes.toLong())

        Result.success()
    }

    private fun performCheck(client: OkHttpClient, monitoredUrl: MonitoredUrl): CheckResult {
        return try {
            val needsContent = monitoredUrl.expectedContent != null
            val request = Request.Builder()
                .url(monitoredUrl.url)
                .apply {
                    if (needsContent) get() else head()
                }
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                val responseCode = resp.code
                val responseBody = if (needsContent) resp.body?.string().orEmpty() else ""

                when {
                    responseCode != monitoredUrl.expectedHttpCode -> {
                        CheckResult.Failed("Expected HTTP ${monitoredUrl.expectedHttpCode}, got $responseCode")
                    }
                    monitoredUrl.expectedContent != null &&
                        !responseBody.contains(monitoredUrl.expectedContent) -> {
                        CheckResult.Failed("Expected content not found in response")
                    }
                    else -> CheckResult.Success
                }
            }
        } catch (e: SocketTimeoutException) {
            CheckResult.Failed("Request timed out after ${monitoredUrl.timeoutSeconds}s")
        } catch (e: IOException) {
            CheckResult.Failed("Connection failed: ${e.message}")
        } catch (e: Exception) {
            CheckResult.Failed("Unexpected error: ${e.message}")
        }
    }

    private suspend fun markAlert(
        dao: com.montheweb.data.db.MonitoredUrlDao,
        notificationHelper: NotificationHelper,
        urlId: Long,
        urlName: String,
        error: String
    ) {
        dao.updateCheckResult(urlId, isAlerted = true, lastCheckTime = System.currentTimeMillis(), lastError = error)
        notificationHelper.showAlertNotification(urlId, urlName, error)
    }

    private suspend fun markRecovered(
        dao: com.montheweb.data.db.MonitoredUrlDao,
        notificationHelper: NotificationHelper,
        urlId: Long
    ) {
        dao.updateCheckResult(urlId, isAlerted = false, lastCheckTime = System.currentTimeMillis(), lastError = null)
        notificationHelper.cancelNotification(urlId)
    }
}

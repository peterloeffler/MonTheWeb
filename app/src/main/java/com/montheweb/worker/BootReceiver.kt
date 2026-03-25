package com.montheweb.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.montheweb.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.montheweb.data.repository.UrlRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = UrlRepository(context)
                val urls = repository.getAllUrls()
                repository.scheduleAllUrlChecks(urls)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

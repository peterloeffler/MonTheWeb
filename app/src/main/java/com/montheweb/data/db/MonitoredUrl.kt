package com.montheweb.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_urls")
data class MonitoredUrl(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val scrapeIntervalMinutes: Int = 10,
    val timeoutSeconds: Int = 5,
    val expectedHttpCode: Int = 200,
    val expectedContent: String? = null,
    val sortOrder: Int = 0,
    val isAlerted: Boolean = false,
    val isChecking: Boolean = false,
    val lastCheckTime: Long? = null,
    val lastError: String? = null
)

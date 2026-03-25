package com.montheweb.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredUrlDao {

    @Query("SELECT * FROM monitored_urls ORDER BY sortOrder ASC")
    fun getAllUrlsFlow(): Flow<List<MonitoredUrl>>

    @Query("SELECT * FROM monitored_urls ORDER BY sortOrder ASC")
    suspend fun getAllUrls(): List<MonitoredUrl>

    @Query("SELECT * FROM monitored_urls WHERE id = :id")
    suspend fun getUrlById(id: Long): MonitoredUrl?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(url: MonitoredUrl): Long

    @Update
    suspend fun update(url: MonitoredUrl)

    @Delete
    suspend fun delete(url: MonitoredUrl)

    @Query("UPDATE monitored_urls SET isAlerted = :isAlerted, lastCheckTime = :lastCheckTime, lastError = :lastError WHERE id = :id")
    suspend fun updateCheckResult(id: Long, isAlerted: Boolean, lastCheckTime: Long, lastError: String?)

    @Query("SELECT MAX(sortOrder) FROM monitored_urls")
    suspend fun getMaxSortOrder(): Int?

    @Update
    suspend fun updateAll(urls: List<MonitoredUrl>)
}

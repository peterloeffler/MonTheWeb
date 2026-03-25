package com.montheweb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MonitoredUrl::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun monitoredUrlDao(): MonitoredUrlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "montheweb.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

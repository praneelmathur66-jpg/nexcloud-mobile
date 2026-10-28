package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedFileEntity::class], version = 1, exportSchema = false)
abstract class NexCloudDatabase : RoomDatabase() {
    abstract fun cachedFileDao(): CachedFileDao

    companion object {
        @Volatile
        private var INSTANCE: NexCloudDatabase? = null

        fun getInstance(context: Context): NexCloudDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexCloudDatabase::class.java,
                    "nexcloud.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

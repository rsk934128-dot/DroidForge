package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [BookmarkedRepoEntity::class, CachedWorkflowRunEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun bookmarkedRepoDao(): BookmarkedRepoDao
  abstract fun cachedWorkflowRunDao(): CachedWorkflowRunDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "droidforge.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { instance = it }
        }
  }
}

package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room database for MoodLog.
 * Uses a thread-safe singleton so the whole app shares one database connection.
 */
@Database(
    entities = [MoodExpenseEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun moodExpenseDao(): MoodExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mood_expense_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

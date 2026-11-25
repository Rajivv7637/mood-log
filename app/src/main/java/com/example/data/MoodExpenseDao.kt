package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) defining all SQLite queries for MoodLog.
 * Returning Flow lets the UI update automatically whenever entries are added or deleted.
 */
@Dao
interface MoodExpenseDao {

    // Fetches every saved entry sorted newest first
    @Query("SELECT * FROM mood_expense_entries ORDER BY timestampMillis DESC, id DESC")
    fun getAllEntriesNewestFirst(): Flow<List<MoodExpenseEntry>>

    // Fetches recent entries since a given timestamp (used for Gemini 7-day weekly insight)
    @Query("SELECT * FROM mood_expense_entries WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC, id DESC")
    suspend fun getEntriesSince(sinceMillis: Long): List<MoodExpenseEntry>

    // Inserts a new entry on a background thread (suspend function)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MoodExpenseEntry): Long

    // Deletes an entry by its unique ID when the user taps the delete button
    @Query("DELETE FROM mood_expense_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}

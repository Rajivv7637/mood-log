package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Repository layer sitting between Room DAO and the ViewModel.
 * Keeps database details separated from UI logic.
 */
open class MoodExpenseRepository(
    private val dao: MoodExpenseDao
) {
    // Live stream of all entries sorted newest first
    open val allEntries: Flow<List<MoodExpenseEntry>> = dao.getAllEntriesNewestFirst()

    // Saves a new mood & expense check-in into Room
    open suspend fun addEntry(
        mood: MoodType,
        note: String,
        amountInr: Double,
        timestampMillis: Long = System.currentTimeMillis()
    ): Long {
        val cleanNote = note.trim().ifBlank { "Quick check-in" }
        val entry = MoodExpenseEntry(
            moodName = mood.name,
            note = cleanNote,
            amountInr = amountInr,
            timestampMillis = timestampMillis
        )
        return dao.insertEntry(entry)
    }

    // Restores a previously deleted entry (used by the Undo Snackbar)
    open suspend fun restoreEntry(entry: MoodExpenseEntry) {
        dao.insertEntry(entry)
    }

    // Removes an entry permanently by ID
    open suspend fun deleteEntryById(id: Long) {
        dao.deleteEntryById(id)
    }

    // Fetches entries logged in the last 7 days for Gemini weekly insight analysis
    open suspend fun getRecentEntriesForWeek(
        nowMillis: Long = System.currentTimeMillis()
    ): List<MoodExpenseEntry> {
        val sevenDaysAgo = nowMillis - TimeUnit.DAYS.toMillis(7)
        return dao.getEntriesSince(sevenDaysAgo)
    }
}

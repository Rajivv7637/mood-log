package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing one row in the local SQLite table "mood_expense_entries".
 * Each check-in stores the user's mood, optional note, ₹ expense amount, and timestamp.
 */
@Entity(tableName = "mood_expense_entries")
data class MoodExpenseEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val moodName: String,
    val note: String,
    val amountInr: Double,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    // Helper property to turn the stored string back into our MoodType enum
    val mood: MoodType
        get() = MoodType.fromStoredName(moodName)
}

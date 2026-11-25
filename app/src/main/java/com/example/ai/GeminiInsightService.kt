package com.example.ai

import com.example.data.MoodExpenseEntry
import com.example.util.formatEntryTimestamp
import com.example.util.formatInr
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.withTimeout

/**
 * Abstraction for generating weekly mood & spending insights.
 * Keeping an interface makes it easy to test the ViewModel without network calls.
 */
interface WeeklyInsightService {
    suspend fun generateWeeklyInsight(entries: List<MoodExpenseEntry>): String
}

/**
 * Uses Firebase AI Logic (Gemini 3.8 Flash) to summarize recent mood and spending patterns.
 * Because Firebase AI Logic uses App Check, no client-side GEMINI_API_KEY is needed.
 */
class GeminiInsightService : WeeklyInsightService {

    // Lazily initialize the Gemini model so startup stays fast and safe
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    override suspend fun generateWeeklyInsight(entries: List<MoodExpenseEntry>): String {
        val logLines = entries.take(40).joinToString(separator = "\n") { entry ->
            val timeText = formatEntryTimestamp(entry.timestampMillis)
            val spendText = if (entry.amountInr > 0.0) formatInr(entry.amountInr) else "₹0"
            "- [$timeText] Mood: ${entry.mood.label} ${entry.mood.emoji} | Spend: $spendText | Note: ${entry.note}"
        }

        val prompt = """
            You are a warm, encouraging personal reflection buddy for an Indian user's Mood & Expense Log app.
            Analyze these recent entries from the past week and write a short, friendly insight (3 to 4 sentences max).
            Highlight:
            1. Their most common mood or mood shift.
            2. Any connection between their moods and spending in Indian Rupees (₹).
            3. One gentle, practical tip for the week ahead.
            Keep the tone supportive, concise, and easy to read. Do not use markdown tables or heavy formatting.

            Recent Entries:
            $logLines
        """.trimIndent()

        // Allow up to 60 seconds for the AI request to complete
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }

        val text = response.text?.trim()
        if (text.isNullOrBlank()) {
            throw IllegalStateException("Gemini returned an empty insight. Please try again.")
        }
        return text
    }
}

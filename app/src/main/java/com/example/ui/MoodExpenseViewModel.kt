package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiInsightService
import com.example.ai.WeeklyInsightService
import com.example.data.MoodExpenseEntry
import com.example.data.MoodExpenseRepository
import com.example.data.MoodType
import com.example.util.isTimestampToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MoodExpenseViewModel"

/**
 * Represents the 4 states of our Gemini Weekly Insight card.
 */
sealed interface InsightUiState {
    data object Idle : InsightUiState
    data object Loading : InsightUiState
    data class Success(val summary: String) : InsightUiState
    data class Error(val message: String) : InsightUiState
}

/**
 * Holds everything the Mood & Expense Log screen needs to display.
 */
data class MoodExpenseUiState(
    val selectedMood: MoodType = MoodType.HAPPY,
    val noteInput: String = "",
    val amountInput: String = "",
    val amountError: String? = null,
    val entries: List<MoodExpenseEntry> = emptyList(),
    val todayTotalInr: Double = 0.0,
    val todayEntryCount: Int = 0,
    val totalEntryCount: Int = 0,
    val insightState: InsightUiState = InsightUiState.Idle
)

/**
 * ViewModel holding form inputs, Room entries, daily summary math, and Gemini AI insights.
 * Survives screen rotations and keeps business logic cleanly out of Composables.
 */
class MoodExpenseViewModel(
    private val repository: MoodExpenseRepository,
    private val insightService: WeeklyInsightService = GeminiInsightService()
) : ViewModel() {

    private data class ComposerDraft(
        val selectedMood: MoodType = MoodType.HAPPY,
        val noteInput: String = "",
        val amountInput: String = "",
        val amountError: String? = null
    )

    private val _draft = MutableStateFlow(ComposerDraft())
    private val _insightState = MutableStateFlow<InsightUiState>(InsightUiState.Idle)

    // Combine local draft inputs, Room database stream, and AI state into one reactive StateFlow
    val uiState: StateFlow<MoodExpenseUiState> = combine(
        _draft,
        repository.allEntries,
        _insightState
    ) { draft, entries, insight ->
        val now = System.currentTimeMillis()
        val todayEntries = entries.filter { isTimestampToday(it.timestampMillis, now) }
        val todayTotal = todayEntries.sumOf { it.amountInr }

        MoodExpenseUiState(
            selectedMood = draft.selectedMood,
            noteInput = draft.noteInput,
            amountInput = draft.amountInput,
            amountError = draft.amountError,
            entries = entries,
            todayTotalInr = todayTotal,
            todayEntryCount = todayEntries.size,
            totalEntryCount = entries.size,
            insightState = insight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoodExpenseUiState()
    )

    fun onMoodSelected(mood: MoodType) {
        _draft.update { it.copy(selectedMood = mood) }
    }

    fun onNoteChanged(note: String) {
        _draft.update { it.copy(noteInput = note) }
    }

    fun onAmountChanged(amountText: String) {
        // Clear any prior error immediately when the user edits the expense field
        val trimmed = amountText.trim()
        val liveError = when {
            trimmed.isEmpty() -> null
            trimmed == "-" -> "Expense amount cannot be negative."
            else -> {
                val parsed = trimmed.toDoubleOrNull()
                if (parsed != null && parsed < 0.0) {
                    "Expense amount cannot be negative."
                } else {
                    null
                }
            }
        }
        _draft.update { it.copy(amountInput = amountText, amountError = liveError) }
    }

    /**
     * Validates inputs and saves the entry to the local Room database.
     * Returns true if saved successfully so the UI can show confirmation feedback.
     */
    fun addEntry(onSaved: () -> Unit = {}) {
        val current = _draft.value
        val rawAmount = current.amountInput.trim()

        val parsedAmount = if (rawAmount.isEmpty()) {
            0.0
        } else {
            val number = rawAmount.toDoubleOrNull()
            if (number == null) {
                _draft.update {
                    it.copy(amountError = "Please enter a valid number for the expense.")
                }
                return
            }
            if (number < 0.0) {
                _draft.update {
                    it.copy(amountError = "Expense amount cannot be negative.")
                }
                return
            }
            number
        }

        viewModelScope.launch {
            repository.addEntry(
                mood = current.selectedMood,
                note = current.noteInput,
                amountInr = parsedAmount
            )
            // Clear note, amount, and error after saving
            _draft.update {
                it.copy(
                    noteInput = "",
                    amountInput = "",
                    amountError = null
                )
            }
            onSaved()
        }
    }

    /**
     * Deletes an entry from Room and invokes [onDeleted] with the removed item for optional Undo.
     */
    fun deleteEntry(entry: MoodExpenseEntry, onDeleted: (MoodExpenseEntry) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteEntryById(entry.id)
            onDeleted(entry)
        }
    }

    /**
     * Restores an entry if the user taps "Undo" in the Snackbar.
     */
    fun restoreEntry(entry: MoodExpenseEntry) {
        viewModelScope.launch {
            repository.restoreEntry(entry)
        }
    }

    /**
     * Sends recent mood & expense entries from the past 7 days to Gemini via Firebase AI Logic.
     */
    fun fetchWeeklyInsight() {
        viewModelScope.launch {
            _insightState.value = InsightUiState.Loading
            try {
                val recentEntries = repository.getRecentEntriesForWeek()
                if (recentEntries.isEmpty()) {
                    _insightState.value = InsightUiState.Error(
                        "Log at least one entry first so Gemini can summarize your week."
                    )
                    return@launch
                }
                val summary = insightService.generateWeeklyInsight(recentEntries)
                _insightState.value = InsightUiState.Success(summary)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Weekly insight request timed out", e)
                _insightState.value = InsightUiState.Error(
                    "Gemini took too long to respond. Please check your internet connection and try again."
                )
            } catch (e: CancellationException) {
                // Always re-throw CancellationException so coroutine cancellation works properly
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate weekly insight", e)
                _insightState.value = InsightUiState.Error(
                    e.localizedMessage ?: "Could not load weekly insight right now."
                )
            }
        }
    }

    fun dismissInsight() {
        _insightState.value = InsightUiState.Idle
    }

    class Factory(
        private val repository: MoodExpenseRepository,
        private val insightService: WeeklyInsightService = GeminiInsightService()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MoodExpenseViewModel(repository, insightService) as T
        }
    }
}

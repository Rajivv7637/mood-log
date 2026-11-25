package com.example

import com.example.ai.WeeklyInsightService
import com.example.data.MoodExpenseDao
import com.example.data.MoodExpenseEntry
import com.example.data.MoodExpenseRepository
import com.example.data.MoodType
import com.example.ui.InsightUiState
import com.example.ui.MoodExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for MoodLog's Repository, ViewModel validation, and Insight flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeMoodExpenseDao : MoodExpenseDao {
        private val items = MutableStateFlow<List<MoodExpenseEntry>>(emptyList())
        private var nextId = 1L

        override fun getAllEntriesNewestFirst(): Flow<List<MoodExpenseEntry>> {
            return items.map { list ->
                list.sortedWith(
                    compareByDescending<MoodExpenseEntry> { it.timestampMillis }
                        .thenByDescending { it.id }
                )
            }
        }

        override suspend fun getEntriesSince(sinceMillis: Long): List<MoodExpenseEntry> {
            return items.value.filter { it.timestampMillis >= sinceMillis }
        }

        override suspend fun insertEntry(entry: MoodExpenseEntry): Long {
            val assignedId = if (entry.id == 0L) nextId++ else entry.id
            val saved = entry.copy(id = assignedId)
            items.value = items.value.filterNot { it.id == assignedId } + saved
            return assignedId
        }

        override suspend fun deleteEntryById(id: Long) {
            items.value = items.value.filterNot { it.id == id }
        }
    }

    private lateinit var repository: MoodExpenseRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = MoodExpenseRepository(FakeMoodExpenseDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `negative amount shows error and does not save entry`() = runTest {
        val fakeInsight = object : WeeklyInsightService {
            override suspend fun generateWeeklyInsight(entries: List<MoodExpenseEntry>): String = "OK"
        }
        val viewModel = MoodExpenseViewModel(repository, fakeInsight)

        viewModel.onMoodSelected(MoodType.STRESSED)
        viewModel.onAmountChanged("-45")
        viewModel.addEntry()
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.amountError != null }
        assertNotNull(state.amountError)
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `adding entry defaults blank note, calculates today total, clears inputs, and deletes`() = runTest {
        val fakeInsight = object : WeeklyInsightService {
            override suspend fun generateWeeklyInsight(entries: List<MoodExpenseEntry>): String {
                return "You felt Calm and spent ₹120 on coffee."
            }
        }
        val viewModel = MoodExpenseViewModel(repository, fakeInsight)

        viewModel.onMoodSelected(MoodType.CALM)
        viewModel.onNoteChanged("   ")
        viewModel.onAmountChanged("120")
        viewModel.addEntry()
        advanceUntilIdle()

        val afterAdd = viewModel.uiState.first { it.entries.isNotEmpty() }
        assertEquals(1, afterAdd.totalEntryCount)
        assertEquals(120.0, afterAdd.todayTotalInr, 0.01)
        assertEquals("Quick check-in", afterAdd.entries.first().note)
        assertEquals(MoodType.CALM, afterAdd.entries.first().mood)
        assertEquals("", afterAdd.noteInput)
        assertEquals("", afterAdd.amountInput)
        assertNull(afterAdd.amountError)

        // Fetch weekly insight
        viewModel.fetchWeeklyInsight()
        advanceUntilIdle()
        val afterInsight = viewModel.uiState.first { it.insightState is InsightUiState.Success }
        assertEquals(
            "You felt Calm and spent ₹120 on coffee.",
            (afterInsight.insightState as InsightUiState.Success).summary
        )

        // Delete entry
        viewModel.deleteEntry(afterAdd.entries.first())
        advanceUntilIdle()
        val afterDelete = viewModel.uiState.first { it.entries.isEmpty() }
        assertEquals(0, afterDelete.totalEntryCount)
        assertEquals(0.0, afterDelete.todayTotalInr, 0.01)
    }
}

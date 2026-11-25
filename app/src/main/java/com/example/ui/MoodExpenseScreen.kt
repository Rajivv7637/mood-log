package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.MoodExpenseEntry
import com.example.ui.components.EmptyJournalCard
import com.example.ui.components.EntryComposerCard
import com.example.ui.components.EntryHistoryItem
import com.example.ui.components.SummaryHeroCard
import com.example.ui.components.WeeklyInsightCard
import kotlinx.coroutines.launch

/**
 * Top-level screen for MoodLog.
 * Adapts smoothly between phones (single scrollable column) and tablets/foldables (two panes).
 */
@Composable
fun MoodExpenseScreen(
    viewModel: MoodExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val savedMessage = stringResource(R.string.snackbar_entry_saved)
    val deletedMessage = stringResource(R.string.snackbar_entry_deleted)
    val undoLabel = stringResource(R.string.snackbar_undo)

    val handleAddEntry: () -> Unit = {
        viewModel.addEntry {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = savedMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val handleDeleteEntry: (MoodExpenseEntry) -> Unit = { entry ->
        viewModel.deleteEntry(entry) { removed ->
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreEntry(removed)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedLayout = maxWidth >= 840.dp

            if (isExpandedLayout) {
                // Tablet / Foldable two-pane layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ScreenTitleHeader()
                        EntryComposerCard(
                            selectedMood = uiState.selectedMood,
                            noteInput = uiState.noteInput,
                            amountInput = uiState.amountInput,
                            amountError = uiState.amountError,
                            onMoodSelected = viewModel::onMoodSelected,
                            onNoteChanged = viewModel::onNoteChanged,
                            onAmountChanged = viewModel::onAmountChanged,
                            onAddEntry = handleAddEntry
                        )
                        SummaryHeroCard(
                            todayTotalInr = uiState.todayTotalInr,
                            todayEntryCount = uiState.todayEntryCount,
                            totalEntryCount = uiState.totalEntryCount
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "insight_card") {
                            WeeklyInsightCard(
                                insightState = uiState.insightState,
                                onRequestInsight = viewModel::fetchWeeklyInsight,
                                onDismissInsight = viewModel::dismissInsight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item(key = "history_header") {
                            HistoryHeaderRow(entryCount = uiState.totalEntryCount)
                        }

                        if (uiState.entries.isEmpty()) {
                            item(key = "empty_state") {
                                EmptyJournalCard()
                            }
                        } else {
                            items(
                                items = uiState.entries,
                                key = { it.id }
                            ) { entry ->
                                EntryHistoryItem(
                                    entry = entry,
                                    onDeleteClick = { handleDeleteEntry(entry) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Phone / Compact single-column layout centered up to 640dp
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "header") {
                            ScreenTitleHeader()
                        }

                        // 1. Mood Picker + 2. Note & ₹ Expense Input + 3. Add Entry Button
                        item(key = "composer") {
                            EntryComposerCard(
                                selectedMood = uiState.selectedMood,
                                noteInput = uiState.noteInput,
                                amountInput = uiState.amountInput,
                                amountError = uiState.amountError,
                                onMoodSelected = viewModel::onMoodSelected,
                                onNoteChanged = viewModel::onNoteChanged,
                                onAmountChanged = viewModel::onAmountChanged,
                                onAddEntry = handleAddEntry
                            )
                        }

                        // 4a. Daily Total & Entry Count Summary Card
                        item(key = "summary_card") {
                            SummaryHeroCard(
                                todayTotalInr = uiState.todayTotalInr,
                                todayEntryCount = uiState.todayEntryCount,
                                totalEntryCount = uiState.totalEntryCount
                            )
                        }

                        // 5. Gemini AI Weekly Insight Card
                        item(key = "weekly_insight") {
                            WeeklyInsightCard(
                                insightState = uiState.insightState,
                                onRequestInsight = viewModel::fetchWeeklyInsight,
                                onDismissInsight = viewModel::dismissInsight
                            )
                        }

                        // 4b. Saved Entries List Header
                        item(key = "history_header") {
                            HistoryHeaderRow(entryCount = uiState.totalEntryCount)
                        }

                        if (uiState.entries.isEmpty()) {
                            item(key = "empty_state") {
                                EmptyJournalCard()
                            }
                        } else {
                            items(
                                items = uiState.entries,
                                key = { it.id }
                            ) { entry ->
                                EntryHistoryItem(
                                    entry = entry,
                                    onDeleteClick = { handleDeleteEntry(entry) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenTitleHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryHeaderRow(entryCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.history_section_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$entryCount saved",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

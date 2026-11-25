package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MoodType

/**
 * Main entry composer card containing:
 * 1. The 5-button Mood Picker
 * 2. Optional short note input
 * 3. Optional ₹ expense input with negative-number validation
 * 4. Primary "Add Entry" button
 */
@Composable
fun EntryComposerCard(
    selectedMood: MoodType,
    noteInput: String,
    amountInput: String,
    amountError: String?,
    onMoodSelected: (MoodType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            MoodPickerSection(
                selectedMood = selectedMood,
                onMoodSelected = onMoodSelected
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Optional short note field (defaults to "Quick check-in" if left blank)
            OutlinedTextField(
                value = noteInput,
                onValueChange = onNoteChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                label = { Text(stringResource(R.string.note_label)) },
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Optional expense amount field in Indian Rupees (₹)
            OutlinedTextField(
                value = amountInput,
                onValueChange = onAmountChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_input"),
                label = { Text(stringResource(R.string.expense_label)) },
                placeholder = { Text(stringResource(R.string.expense_placeholder)) },
                prefix = {
                    Text(
                        text = "${stringResource(R.string.rupee_symbol)} ",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                isError = amountError != null,
                supportingText = amountError?.let { errorMessage ->
                    {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("expense_error_text")
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onAddEntry()
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onAddEntry()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("add_entry_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_entry_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

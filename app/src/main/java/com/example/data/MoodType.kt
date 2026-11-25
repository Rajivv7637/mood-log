package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MoodCalmAccent
import com.example.ui.theme.MoodCalmTint
import com.example.ui.theme.MoodHappyAccent
import com.example.ui.theme.MoodHappyTint
import com.example.ui.theme.MoodLowAccent
import com.example.ui.theme.MoodLowTint
import com.example.ui.theme.MoodOkayAccent
import com.example.ui.theme.MoodOkayTint
import com.example.ui.theme.MoodStressedAccent
import com.example.ui.theme.MoodStressedTint

/**
 * Represents the 5 selectable moods in MoodLog.
 * Storing an enum makes mood selection type-safe and easy to map to colors and emojis.
 */
enum class MoodType(
    val label: String,
    val emoji: String,
    val badgeTint: Color,
    val accentColor: Color
) {
    HAPPY("Happy", "😊", MoodHappyTint, MoodHappyAccent),
    CALM("Calm", "😌", MoodCalmTint, MoodCalmAccent),
    OKAY("Okay", "😐", MoodOkayTint, MoodOkayAccent),
    LOW("Low", "😔", MoodLowTint, MoodLowAccent),
    STRESSED("Stressed", "😤", MoodStressedTint, MoodStressedAccent);

    companion object {
        // Safely converts a saved database string back into a MoodType enum
        fun fromStoredName(name: String): MoodType {
            return entries.firstOrNull { it.name == name } ?: OKAY
        }
    }
}

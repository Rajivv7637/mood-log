package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.AppDatabase
import com.example.data.MoodExpenseRepository
import com.example.ui.MoodExpenseScreen
import com.example.ui.MoodExpenseViewModel
import com.example.ui.theme.MoodLogTheme

/**
 * Main entry point of the MoodLog app.
 * Configures Firebase App Check first, then wires the Room database into our ViewModel and UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Initialize Firebase App Check with the launch intent BEFORE any UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable full-screen edge-to-edge rendering
        enableEdgeToEdge()

        // STEP 3: Create our local Room database and repository
        val database = AppDatabase.getInstance(applicationContext)
        val repository = MoodExpenseRepository(database.moodExpenseDao())

        // STEP 4: Render our Jetpack Compose UI
        setContent {
            MoodLogTheme {
                val viewModel: MoodExpenseViewModel = viewModel(
                    factory = MoodExpenseViewModel.Factory(repository)
                )
                MoodExpenseScreen(viewModel = viewModel)
            }
        }
    }
}

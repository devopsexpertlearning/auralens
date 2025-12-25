package com.example.auralens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auralens.data.GeminiClient
import com.example.auralens.data.VisionRepository
import com.example.auralens.ui.HomeScreen
import com.example.auralens.ui.SettingsScreen
import com.example.auralens.ui.VisionViewModel
import com.example.auralens.ui.theme.AuraLensTheme
import com.example.auralens.util.CameraManager
import com.example.auralens.util.HapticManager
import com.example.auralens.util.SpeechManager
import com.example.auralens.util.TTSManager

class MainActivity : ComponentActivity() {

    // TODO: INSERT YOUR GEMINI API KEY HERE
    private val GEMINI_API_KEY = "AIzaSyB5Nk5_HArZ4iX9oyKia7DhkDyJ5N0r6ek"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual Dependency Injection
        val geminiClient = GeminiClient(GEMINI_API_KEY)
        val repository = VisionRepository(geminiClient)
        val cameraManager = CameraManager(this)
        val ttsManager = TTSManager(this)
        val speechManager = SpeechManager(this)

        val hapticManager = HapticManager(this)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(VisionViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return VisionViewModel(repository, ttsManager, speechManager, hapticManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[VisionViewModel::class.java]

        setContent {
            AuraLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                cameraManager = cameraManager,
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

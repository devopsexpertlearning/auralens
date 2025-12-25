package com.example.auralens.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auralens.data.VisionRepository
import com.example.auralens.util.SpeechManager
import com.example.auralens.util.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.mutableStateListOf
import com.example.auralens.util.CameraManager
import com.example.auralens.util.HapticManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class VisionUiState {
    object Idle : VisionUiState()
    object Loading : VisionUiState()
    data class Success(val text: String) : VisionUiState()
    data class Error(val message: String) : VisionUiState()
}

class VisionViewModel(
    private val repository: VisionRepository,
    private val ttsManager: TTSManager,
    private val speechManager: SpeechManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    // Smart History
    val history = mutableStateListOf<String>()

    // Live Mode
    var isLiveMode by mutableStateOf(false)
        private set
    
    private var liveModeJob: Job? = null

    // For "Find Object" mode
    var targetObject by mutableStateOf("")
        private set

    // For "Ask Q&A" mode
    var userQuestion by mutableStateOf("")
        private set
    
    // Settings
    var selectedModelName by mutableStateOf("gemini-3-pro-preview")
    var isDetailedDescription by mutableStateOf(false)

    fun onModelSelected(name: String) {
        selectedModelName = name
        repository.setModelName(name)
    }

    fun onDetailedDescriptionToggled(enabled: Boolean) {
        isDetailedDescription = enabled
        repository.setDetailedDescriptions(enabled)
    }

    fun updateTargetObject(name: String) {
        targetObject = name
    }
    
    fun updateQuestion(text: String) {
        userQuestion = text
    }

    fun toggleLiveMode(cameraManager: CameraManager) {
        isLiveMode = !isLiveMode
        if (isLiveMode) {
            startLiveModeLoop(cameraManager)
        } else {
            stopLiveModeLoop()
        }
    }
    
    private fun startLiveModeLoop(cameraManager: CameraManager) {
        speak("Live guide enabled.")
        liveModeJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val bitmap = cameraManager.takePhoto()
                    // Don't show loading UI for live mode to keep it subtle
                    val result = repository.describeScene(bitmap)
                    if (result.isSuccess) {
                        val text = result.getOrNull() ?: ""
                        if (text.isNotBlank()) {
                            addToHistory(text)
                            speak(text)
                        }
                    }
                    delay(5000) // Every 5 seconds
                } catch (e: Exception) {
                    // Ignore errors in live loop to prevent crash
                }
            }
        }
    }
    
    private fun stopLiveModeLoop() {
        speak("Live guide disabled.")
        liveModeJob?.cancel()
        liveModeJob = null
    }

    fun describeScene(image: Bitmap) {
        _uiState.value = VisionUiState.Loading
        viewModelScope.launch {
            val result = repository.describeScene(image)
            handleResult(result)
        }
    }

    fun readText(image: Bitmap) {
        _uiState.value = VisionUiState.Loading
        speak("Reading text...")
        viewModelScope.launch {
            val result = repository.readText(image)
            handleResult(result)
        }
    }

    fun findObject(image: Bitmap) {
        if (targetObject.isBlank()) {
            _uiState.value = VisionUiState.Error("Please say or type object name first")
            speak("Please tell me what to find first.")
            return
        }
        _uiState.value = VisionUiState.Loading
        viewModelScope.launch {
            val result = repository.findObject(image, targetObject)
            
            // Check for success to trigger Haptics
            if (result.isSuccess) {
                val text = result.getOrDefault("")
                if (text.contains("\"found\": true", ignoreCase = true) || text.contains("found", ignoreCase = true)) {
                    hapticManager.vibrateSuccess()
                } else {
                     hapticManager.vibrateError()
                }
                handleResult(result)
            } else {
                hapticManager.vibrateError()
                handleResult(result)
            }
        }
    }

    fun askAboutPhoto(image: Bitmap) {
        if (userQuestion.isBlank()) {
             _uiState.value = VisionUiState.Error("Please ask a question first")
             speak("Please ask a question first.")
             return
        }
        _uiState.value = VisionUiState.Loading
        viewModelScope.launch {
            val result = repository.askAboutPhoto(image, userQuestion)
            handleResult(result)
        }
    }
    
    private fun handleResult(result: Result<String>) {
        if (result.isSuccess) {
            val text = result.getOrDefault("No result")
            _uiState.value = VisionUiState.Success(text)
            addToHistory(text)
            speak(text)
        } else {
            val error = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
            _uiState.value = VisionUiState.Error(error)
            hapticManager.vibrateError()
            speak("Something went wrong. $error")
        }
    }
    
    private fun addToHistory(text: String) {
        if (history.size > 20) {
            history.removeAt(0)
        }
        history.add(text)
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }
    
    fun startListening() {
        speechManager.startListening()
    }
    
    fun stopListening() {
        speechManager.stopListening()
    }
    
    val speechResult = speechManager.speechResult

    fun clearError() {
        if (_uiState.value is VisionUiState.Error) {
            _uiState.value = VisionUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        speechManager.destroy()
        stopLiveModeLoop()
    }
}

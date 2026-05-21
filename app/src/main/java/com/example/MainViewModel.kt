package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voice.ActionExecutor
import com.example.voice.SpeechManager
import com.example.voice.SpeechState
import com.example.voice.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechManager(application)
    private val actionExecutor = ActionExecutor(application)
    private val ttsManager = TTSManager(application)

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.Idle -> {
                        _uiState.value = _uiState.value.copy(isListening = false, statusText = "Ready")
                    }
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = true,
                            statusText = "Listening...",
                            recognizedText = state.partialText
                        )
                    }
                    is SpeechState.Processing -> {
                         _uiState.value = _uiState.value.copy(isListening = false, statusText = "Processing...")
                    }
                    is SpeechState.Success -> {
                        val command = state.text
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            statusText = "Executed",
                            recognizedText = command
                        )
                        processCommand(command)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            statusText = "Error",
                            recognizedText = state.message
                        )
                        ttsManager.speak(state.message) // Speak out error
                    }
                }
            }
        }
    }

    fun startListening() {
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    private fun processCommand(command: String) {
        val resultMessage = actionExecutor.executeCommand(command)
        
        // Show result message and speak it
        _uiState.value = _uiState.value.copy(
            responseMessage = resultMessage
        )
        ttsManager.speak(resultMessage)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}

data class VoiceUiState(
    val isListening: Boolean = false,
    val statusText: String = "Ready",
    val recognizedText: String = "",
    val responseMessage: String = ""
)

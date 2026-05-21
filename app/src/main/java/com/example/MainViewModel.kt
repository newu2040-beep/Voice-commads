package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voice.ActionExecutor
import com.example.voice.SpeechManager
import com.example.voice.SpeechState
import com.example.voice.TTSManager
import com.example.voice.data.AppDatabase
import com.example.voice.data.CommandHistory
import com.example.voice.data.CommandHistoryRepository
import com.example.voice.data.CustomCommand
import com.example.voice.data.CustomCommandRepository
import com.example.voice.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val speechManager = SpeechManager(application)
    private val actionExecutor = ActionExecutor(application)
    private val ttsManager = TTSManager(application)
    
    private val customCommandDao = AppDatabase.getDatabase(application).customCommandDao()
    private val customCommandRepo = CustomCommandRepository(customCommandDao)
    
    private val commandHistoryDao = AppDatabase.getDatabase(application).commandHistoryDao()
    private val commandHistoryRepo = CommandHistoryRepository(commandHistoryDao)

    val settingsRepo = SettingsRepository(application)
    
    val allCustomCommands: StateFlow<List<CustomCommand>> = customCommandRepo.allCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandHistory: StateFlow<List<CommandHistory>> = commandHistoryRepo.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeIndex: StateFlow<Int> = settingsRepo.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isDarkMode: StateFlow<Boolean> = settingsRepo.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
                         _uiState.value = _uiState.value.copy(isListening = false, statusText = "Processing...", recognizedText = "Processing")
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
        val lowerCommand = command.lowercase().trim()
        val customCommands = allCustomCommands.value
        
        val customMatched = customCommands.find { lowerCommand.contains(it.phrase.lowercase()) }
        
        val resultMessage = if (customMatched != null) {
             when (customMatched.actionType) {
                 "APP" -> actionExecutor.executeCommand("open ${customMatched.actionData}")
                 "RESPONSE" -> customMatched.actionData
                 else -> "Custom command action not recognized."
             }
        } else {
             actionExecutor.executeCommand(command)
        }
        
        viewModelScope.launch {
            commandHistoryRepo.insert(CommandHistory(commandText = command, responseText = resultMessage))
        }
        
        _uiState.value = _uiState.value.copy(
            responseMessage = resultMessage
        )
        ttsManager.speak(resultMessage)
    }

    fun addCustomCommand(phrase: String, actionType: String, actionData: String) {
        viewModelScope.launch {
            customCommandRepo.insert(CustomCommand(phrase = phrase, actionType = actionType, actionData = actionData))
        }
    }

    fun deleteCustomCommand(id: Int) {
        viewModelScope.launch {
            customCommandRepo.deleteById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            commandHistoryRepo.clearAll()
        }
    }

    fun setTheme(themeInd: Int) {
        viewModelScope.launch { settingsRepo.setTheme(themeInd) }
    }

    fun setDarkMode(dark: Boolean) {
         viewModelScope.launch { settingsRepo.setDarkMode(dark) }
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

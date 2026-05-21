package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) : RecognitionListener {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent
    
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechManager)
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to create SpeechRecognizer: ${e.message}")
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Prioritize offline recognition where possible
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    fun startListening() {
        _speechState.value = SpeechState.Listening("Listening...")
        try {
            val sr = speechRecognizer
            if (sr != null) {
                sr.startListening(recognizerIntent)
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@SpeechManager)
                    startListening(recognizerIntent)
                }
            }
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Voice helper is unavailable on this device.")
            Log.e("SpeechManager", "Failed to start listening: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to stop listening: ${e.message}")
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to destroy: ${e.message}")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    
    override fun onBeginningOfSpeech() {}
    
    override fun onRmsChanged(rmsdB: Float) {}
    
    override fun onBufferReceived(buffer: ByteArray?) {}
    
    override fun onEndOfSpeech() {
        if (_speechState.value is SpeechState.Listening) {
             _speechState.value = SpeechState.Processing
        }
    }
    
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        Log.e("SpeechManager", "Error: $errorMessage")
        _speechState.value = SpeechState.Error(errorMessage)
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _speechState.value = SpeechState.Success(text)
        } else {
            _speechState.value = SpeechState.Error("Could not recognize speech.")
        }
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
             _speechState.value = SpeechState.Listening(matches[0])
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

sealed class SpeechState {
    object Idle : SpeechState()
    data class Listening(val partialText: String) : SpeechState()
    object Processing : SpeechState()
    data class Success(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

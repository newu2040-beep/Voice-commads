package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Language not supported")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("TTSManager", "Initialization failed")
        }
    }

    fun setVoiceStyle(type: Int) {
        when (type) {
            1 -> { tts?.setPitch(1.3f); tts?.setSpeechRate(1.0f) } // Sweet
            2 -> { tts?.setPitch(0.7f); tts?.setSpeechRate(0.85f) } // Mature
            3 -> { tts?.setPitch(1.1f); tts?.setSpeechRate(1.1f) } // Friendly
            4 -> { tts?.setPitch(0.9f); tts?.setSpeechRate(0.85f) } // Calm
            5 -> { tts?.setPitch(1.0f); tts?.setSpeechRate(1.0f) } // Natural
            else -> { tts?.setPitch(1.0f); tts?.setSpeechRate(1.0f) } // Default
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TTSManager", "TTS Not initialized")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

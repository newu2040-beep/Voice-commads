package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.MainActivity
import com.example.voice.ActionExecutor
import com.example.voice.SpeechManager
import com.example.voice.SpeechState
import com.example.voice.TTSManager
import com.example.voice.data.AppDatabase
import com.example.voice.data.CommandHistory
import com.example.voice.data.CommandHistoryRepository
import com.example.voice.data.CustomCommandRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FloatingVoiceService : LifecycleService() {

    private lateinit var floatingViewHelper: FloatingViewHelper
    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TTSManager
    private lateinit var actionExecutor: ActionExecutor
    
    private lateinit var customCommandRepo: CustomCommandRepository
    private lateinit var commandHistoryRepo: CommandHistoryRepository
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        
        speechManager = SpeechManager(this)
        ttsManager = TTSManager(this)
        actionExecutor = ActionExecutor(this)
        
        val db = AppDatabase.getDatabase(this)
        customCommandRepo = CustomCommandRepository(db.customCommandDao())
        commandHistoryRepo = CommandHistoryRepository(db.commandHistoryDao())

        serviceScope.launch {
            val settingsRepo = com.example.voice.data.SettingsRepository(this@FloatingVoiceService)
            settingsRepo.voiceFlow.collect { voiceIdx ->
                ttsManager.setVoiceStyle(voiceIdx)
            }
        }

        floatingViewHelper = FloatingViewHelper(this, {
             if (speechManager.speechState.value is SpeechState.Listening || speechManager.speechState.value is SpeechState.Processing) {
                 speechManager.stopListening()
             } else {
                 speechManager.startListening()
             }
        }, speechManager.speechState)
        
        floatingViewHelper.show()

        serviceScope.launch {
            speechManager.speechState.collect { state ->
                if (state is SpeechState.Success) {
                    processCommand(state.text)
                } else if (state is SpeechState.Error) {
                    ttsManager.speak(state.message) // Speak out error
                }
            }
        }
    }
    
    private suspend fun processCommand(command: String) {
        val lowerCommand = command.lowercase().trim()
        val customCommands = customCommandRepo.allCommands.firstOrNull() ?: emptyList()
        
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
        
        commandHistoryRepo.insert(CommandHistory(commandText = command, responseText = resultMessage))
        ttsManager.speak(resultMessage)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(1, createNotification())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingViewHelper.hide()
        speechManager.destroy()
        ttsManager.shutdown()
        serviceJob.cancel()
    }

    private fun createNotification(): Notification {
        val channelId = "floating_voice_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Aura AI Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating voice assistant alive"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aura AI Offline Assist")
            .setContentText("Tap the floating icon to use voice commands.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Default icon for mic
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

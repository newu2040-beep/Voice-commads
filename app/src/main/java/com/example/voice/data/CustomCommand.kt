package com.example.voice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val actionType: String, // "APP", "RESPONSE", "PHONE"
    val actionData: String // e.g., "com.spotify.music" or "Hello there!"
)

package com.example.voice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CommandHistory>>

    @Insert
    suspend fun insertHistory(history: CommandHistory)

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

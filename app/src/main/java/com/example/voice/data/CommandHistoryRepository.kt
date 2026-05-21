package com.example.voice.data

import kotlinx.coroutines.flow.Flow

class CommandHistoryRepository(private val dao: CommandHistoryDao) {
    val allHistory: Flow<List<CommandHistory>> = dao.getAllHistory()

    suspend fun insert(history: CommandHistory) = dao.insertHistory(history)

    suspend fun clearAll() = dao.clearHistory()
}

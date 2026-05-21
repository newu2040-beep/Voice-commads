package com.example.voice.data

import kotlinx.coroutines.flow.Flow

class CustomCommandRepository(private val dao: CustomCommandDao) {
    val allCommands: Flow<List<CustomCommand>> = dao.getAllCommands()

    suspend fun insert(command: CustomCommand) = dao.insertCommand(command)

    suspend fun deleteById(id: Int) = dao.deleteCommandById(id)
}

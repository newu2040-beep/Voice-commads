package com.example.voice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY phrase ASC")
    fun getAllCommands(): Flow<List<CustomCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommand)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCommandById(id: Int)
}

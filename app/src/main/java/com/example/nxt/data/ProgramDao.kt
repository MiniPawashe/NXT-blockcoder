package com.example.nxt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Query("SELECT * FROM saved_programs ORDER BY updatedAt DESC")
    fun getAllPrograms(): Flow<List<SavedProgramEntity>>

    @Query("SELECT * FROM saved_programs WHERE id = :id")
    suspend fun getProgramById(id: Long): SavedProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: SavedProgramEntity): Long

    @Update
    suspend fun updateProgram(program: SavedProgramEntity)

    @Delete
    suspend fun deleteProgram(program: SavedProgramEntity)

    @Query("DELETE FROM saved_programs WHERE id = :id")
    suspend fun deleteProgramById(id: Long)

    @Query("SELECT COUNT(*) FROM saved_programs")
    suspend fun getProgramCount(): Int
}

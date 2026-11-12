package com.example.nxt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_programs")
data class SavedProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val jsonContent: String,
    val blockCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isTemplate: Boolean = false
)

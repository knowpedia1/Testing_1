package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long = 0L,
    val role: String, // "user" or "model"
    val textContent: String,
    val imageLocalUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

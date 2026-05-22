package com.example.data

import com.example.data.db.MessageDao
import com.example.data.db.MessageEntity
import com.example.data.db.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val messageDao: MessageDao) {
    val allSessions: Flow<List<ChatSessionEntity>> = messageDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForSession(sessionId)
    }

    suspend fun insertSession(title: String): Long {
        return messageDao.insertSession(ChatSessionEntity(title = title))
    }

    suspend fun deleteSession(sessionId: Long) {
        messageDao.deleteSession(sessionId)
        messageDao.clearHistoryForSession(sessionId)
    }

    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun clearHistoryForSession(sessionId: Long) {
        messageDao.clearHistoryForSession(sessionId)
    }
}

package com.example.repository

import com.example.database.ChatMessages
import com.example.database.Users
import com.example.models.ChatMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

object ChatRepository {
    
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    fun save(userId: Int, message: String): ChatMessage {
        return transaction {
            val id = ChatMessages.insert {
                it[ChatMessages.userId] = userId
                it[ChatMessages.message] = message
            } get ChatMessages.id
            
            (ChatMessages innerJoin Users)
                .select { ChatMessages.id eq id }
                .map { rowToMessage(it) }
                .single()
        }
    }
    
    fun getRecent(limit: Int = 50): List<ChatMessage> {
        return transaction {
            (ChatMessages innerJoin Users)
                .selectAll()
                .orderBy(ChatMessages.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { rowToMessage(it) }
                .reversed()
        }
    }
    
    private fun rowToMessage(row: ResultRow): ChatMessage {
        return ChatMessage(
            id = row[ChatMessages.id],
            userId = row[ChatMessages.userId],
            username = row[Users.username],
            message = row[ChatMessages.message],
            timestamp = row[ChatMessages.createdAt].format(formatter)
        )
    }
}

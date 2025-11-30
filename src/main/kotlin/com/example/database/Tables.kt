package com.example.database

import com.example.models.Role
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// Таблица пользователей
object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20).default(Role.USER.name)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

// Таблица задач
object Tasks : Table("tasks") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 200)
    val description = text("description")
    val completed = bool("completed").default(false)
    val userId = integer("user_id").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

// Таблица сообщений чата
object ChatMessages : Table("chat_messages") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val message = text("message")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(id)
}

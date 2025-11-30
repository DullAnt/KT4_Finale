package com.example.repository

import com.example.database.Tasks
import com.example.models.Task
import com.example.models.TaskRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

object TaskRepository {
    
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    fun create(request: TaskRequest, userId: Int): Task {
        return transaction {
            val id = Tasks.insert {
                it[title] = request.title
                it[description] = request.description
                it[completed] = request.completed
                it[Tasks.userId] = userId
            } get Tasks.id
            
            Tasks.select { Tasks.id eq id }
                .map { rowToTask(it) }
                .single()
        }
    }
    
    fun getAllByUser(userId: Int): List<Task> {
        return transaction {
            Tasks.select { Tasks.userId eq userId }
                .orderBy(Tasks.createdAt, SortOrder.DESC)
                .map { rowToTask(it) }
        }
    }
    
    fun getAll(): List<Task> {
        return transaction {
            Tasks.selectAll()
                .orderBy(Tasks.createdAt, SortOrder.DESC)
                .map { rowToTask(it) }
        }
    }
    
    fun getById(id: Int, userId: Int): Task? {
        return transaction {
            Tasks.select { (Tasks.id eq id) and (Tasks.userId eq userId) }
                .map { rowToTask(it) }
                .singleOrNull()
        }
    }
    
    fun getByIdAdmin(id: Int): Task? {
        return transaction {
            Tasks.select { Tasks.id eq id }
                .map { rowToTask(it) }
                .singleOrNull()
        }
    }
    
    fun filter(userId: Int, completed: Boolean): List<Task> {
        return transaction {
            Tasks.select { (Tasks.userId eq userId) and (Tasks.completed eq completed) }
                .orderBy(Tasks.createdAt, SortOrder.DESC)
                .map { rowToTask(it) }
        }
    }
    
    fun search(userId: Int, query: String): List<Task> {
        return transaction {
            val pattern = "%${query.lowercase()}%"
            Tasks.select { 
                (Tasks.userId eq userId) and 
                ((Tasks.title.lowerCase() like pattern) or (Tasks.description.lowerCase() like pattern))
            }
            .orderBy(Tasks.createdAt, SortOrder.DESC)
            .map { rowToTask(it) }
        }
    }
    
    fun update(id: Int, request: TaskRequest, userId: Int): Task? {
        return transaction {
            val updated = Tasks.update({ (Tasks.id eq id) and (Tasks.userId eq userId) }) {
                it[title] = request.title
                it[description] = request.description
                it[completed] = request.completed
            }
            
            if (updated > 0) {
                Tasks.select { Tasks.id eq id }
                    .map { rowToTask(it) }
                    .singleOrNull()
            } else null
        }
    }
    
    fun delete(id: Int, userId: Int): Boolean {
        return transaction {
            Tasks.deleteWhere { (Tasks.id eq id) and (Tasks.userId eq userId) } > 0
        }
    }
    
    fun deleteAdmin(id: Int): Boolean {
        return transaction {
            Tasks.deleteWhere { Tasks.id eq id } > 0
        }
    }
    
    private fun rowToTask(row: ResultRow): Task {
        return Task(
            id = row[Tasks.id],
            title = row[Tasks.title],
            description = row[Tasks.description],
            completed = row[Tasks.completed],
            userId = row[Tasks.userId],
            createdAt = row[Tasks.createdAt].format(formatter)
        )
    }
}

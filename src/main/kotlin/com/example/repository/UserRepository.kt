package com.example.repository

import com.example.database.Users
import com.example.models.Role
import com.example.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object UserRepository {
    
    fun create(username: String, email: String, password: String, role: Role = Role.USER): User? {
        return transaction {
            // Проверяем уникальность username
            val exists = Users.select { Users.username eq username }.count() > 0
            if (exists) return@transaction null
            
            val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
            
            val id = Users.insert {
                it[Users.username] = username
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.role] = role.name
            } get Users.id
            
            User(id, username, email, role.name)
        }
    }
    
    fun findByUsername(username: String): Pair<User, String>? {
        return transaction {
            Users.select { Users.username eq username }
                .map { 
                    Pair(
                        User(it[Users.id], it[Users.username], it[Users.email], it[Users.role]),
                        it[Users.passwordHash]
                    )
                }
                .singleOrNull()
        }
    }
    
    fun findById(id: Int): User? {
        return transaction {
            Users.select { Users.id eq id }
                .map { User(it[Users.id], it[Users.username], it[Users.email], it[Users.role]) }
                .singleOrNull()
        }
    }
    
    fun findAll(): List<User> {
        return transaction {
            Users.selectAll()
                .map { User(it[Users.id], it[Users.username], it[Users.email], it[Users.role]) }
        }
    }
    
    fun updateRole(userId: Int, role: Role): Boolean {
        return transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.role] = role.name
            } > 0
        }
    }
    
    fun delete(userId: Int): Boolean {
        return transaction {
            Users.deleteWhere { Users.id eq userId } > 0
        }
    }
    
    fun validatePassword(passwordHash: String, password: String): Boolean {
        return BCrypt.checkpw(password, passwordHash)
    }
    
    fun createAdminIfNotExists() {
        transaction {
            val adminExists = Users.select { Users.role eq Role.ADMIN.name }.count() > 0
            if (!adminExists) {
                val passwordHash = BCrypt.hashpw("admin123", BCrypt.gensalt())
                Users.insert {
                    it[username] = "admin"
                    it[email] = "admin@example.com"
                    it[Users.passwordHash] = passwordHash
                    it[role] = Role.ADMIN.name
                }
            }
        }
    }
}

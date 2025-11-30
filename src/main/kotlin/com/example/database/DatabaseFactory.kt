package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init() {
        val dbUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ktor_db"
        val dbUser = System.getenv("DATABASE_USER") ?: "postgres"
        val dbPassword = System.getenv("DATABASE_PASSWORD") ?: "postgres"
        
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            driverClassName = "org.postgresql.Driver"
            username = dbUser
            password = dbPassword
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        
        // Создаём таблицы
        transaction {
            SchemaUtils.create(Users, Tasks, ChatMessages)
        }
    }
    
    // Для тестов с H2
    fun initH2() {
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver"
        )
        
        transaction {
            SchemaUtils.create(Users, Tasks, ChatMessages)
        }
    }
}

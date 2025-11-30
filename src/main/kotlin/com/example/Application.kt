package com.example

import com.example.database.DatabaseFactory
import com.example.plugins.*
import com.example.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Инициализация БД
    DatabaseFactory.init()
    
    // Создаём админа по умолчанию
    UserRepository.createAdminIfNotExists()
    
    // Плагины
    configureLogging()
    configureSerialization()
    configureWebSockets()
    configureCors()
    configureAuthentication()
    configureStatusPages()
    configureRouting()
}

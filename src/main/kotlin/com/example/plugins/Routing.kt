package com.example.plugins

import com.example.models.ApiInfo
import com.example.routes.adminRoutes
import com.example.routes.authRoutes
import com.example.routes.taskRoutes
import com.example.websocket.chatWebSocket
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String)

fun Application.configureRouting() {
    routing {
        // Swagger UI
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
            version = "5.10.3"
        }
        
        // OpenAPI
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        
        // Главная
        get("/") {
            call.respond(HttpStatusCode.OK, ApiInfo(
                success = true,
                message = "Ktor Final API - PostgreSQL + JWT + WebSocket",
                version = "1.0.0",
                docs = "/swagger",
                websocket = "ws://localhost:8080/ws/chat?token=YOUR_JWT"
            ))
        }
        
        // Health check для Docker
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse(status = "ok"))
        }
        
        // Маршруты
        authRoutes()
        taskRoutes()
        adminRoutes()
        chatWebSocket()
    }
}

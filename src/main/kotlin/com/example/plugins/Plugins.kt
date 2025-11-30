package com.example.plugins

import com.example.models.ErrorResponse
import com.example.security.JwtConfig
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.time.Duration

// JSON сериализация
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

// WebSocket
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

// CORS
fun Application.configureCors() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}

// JWT аутентификация
fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("jwt-auth") {
            realm = JwtConfig.realm
            verifier(
                com.auth0.jwt.JWT
                    .require(JwtConfig.algorithm)
                    .withIssuer(JwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        error = "Требуется авторизация. Header: Authorization: Bearer <token>",
                        code = 401
                    )
                )
            }
        }
    }
}

// Обработка ошибок
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = cause.message ?: "Внутренняя ошибка сервера",
                    code = 500
                )
            )
        }
        
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Ресурс не найден: ${call.request.path()}",
                    code = 404
                )
            )
        }
        
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Метод ${call.request.httpMethod.value} не поддерживается",
                    code = 405
                )
            )
        }
    }
}

// Логирование
fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
        
        format { call ->
            val status = call.response.status()
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            
            "HTTP $method $path -> $status (${duration}ms)"
        }
        
        filter { call ->
            !call.request.path().startsWith("/swagger") &&
            !call.request.path().startsWith("/openapi") &&
            !call.request.path().startsWith("/ws")
        }
    }
}

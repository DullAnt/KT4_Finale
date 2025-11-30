package com.example.routes

import com.example.models.*
import com.example.repository.UserRepository
import com.example.security.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private fun parseRegister(text: String): RegisterRequest = json.decodeFromString(text)
private fun parseLogin(text: String): LoginRequest = json.decodeFromString(text)

fun Route.authRoutes() {
    route("/api/auth") {

        post("/register") {
            try {
                val body = call.receiveText()
                val request = parseRegister(body)

                if (request.username.isBlank() || request.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Username и password обязательны"
                    ))
                    return@post
                }

                if (request.password.length < 4) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(
                        success = false,
                        message = "Пароль минимум 4 символа"
                    ))
                    return@post
                }

                val user = UserRepository.create(request.username, request.email, request.password)

                if (user == null) {
                    call.respond(HttpStatusCode.Conflict, AuthResponse(
                        success = false,
                        message = "Username уже существует"
                    ))
                    return@post
                }

                val token = JwtConfig.generateToken(user.id, user.username, user.role)

                call.respond(HttpStatusCode.Created, AuthResponse(
                    success = true,
                    message = "Регистрация успешна",
                    token = token,
                    user = user
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(
                    success = false,
                    message = "Ошибка: ${e.message}"
                ))
            }
        }

        post("/login") {
            try {
                val body = call.receiveText()
                val request = parseLogin(body)

                val result = UserRepository.findByUsername(request.username)

                if (result == null) {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                        success = false,
                        message = "Неверный username или password"
                    ))
                    return@post
                }

                val (user, passwordHash) = result

                if (!UserRepository.validatePassword(passwordHash, request.password)) {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(
                        success = false,
                        message = "Неверный username или password"
                    ))
                    return@post
                }

                val token = JwtConfig.generateToken(user.id, user.username, user.role)

                call.respond(HttpStatusCode.OK, AuthResponse(
                    success = true,
                    message = "Вход выполнен",
                    token = token,
                    user = user
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(
                    success = false,
                    message = "Ошибка: ${e.message}"
                ))
            }
        }
    }
}
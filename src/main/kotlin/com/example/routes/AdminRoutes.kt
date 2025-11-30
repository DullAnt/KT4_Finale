package com.example.routes

import com.example.models.*
import com.example.repository.TaskRepository
import com.example.repository.UserRepository
import com.example.security.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private fun parseRole(text: String): RoleUpdateRequest {
    return json.decodeFromString(RoleUpdateRequest.serializer(), text)
}

fun Route.adminRoutes() {
    authenticate("jwt-auth") {
        route("/api/admin") {

            intercept(ApplicationCallPipeline.Call) {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.let { JwtConfig.getRole(it.payload) }

                if (role != Role.ADMIN.name) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse(
                        error = "Доступ только для администраторов",
                        code = 403
                    ))
                    finish()
                }
            }

            get("/users") {
                val users = UserRepository.findAll()
                call.respond(HttpStatusCode.OK, UserListResponse(
                    success = true,
                    message = "Пользователей: ${users.size}",
                    data = users
                ))
            }

            get("/users/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, UserResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@get
                }

                val user = UserRepository.findById(id)

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, UserResponse(
                        success = false,
                        message = "Пользователь не найден"
                    ))
                    return@get
                }

                call.respond(HttpStatusCode.OK, UserResponse(
                    success = true,
                    message = "OK",
                    data = user
                ))
            }

            put("/users/{id}/role") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    val response = MessageResponse(success = false, message = "Некорректный ID")
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@put
                }

                try {
                    val body = call.receiveText()
                    val request = parseRole(body)

                    val role = try {
                        Role.valueOf(request.role.uppercase())
                    } catch (_: Exception) {
                        val response = MessageResponse(success = false, message = "Некорректная роль. Допустимые: USER, ADMIN")
                        call.respond(HttpStatusCode.BadRequest, response)
                        return@put
                    }

                    val updated = UserRepository.updateRole(id, role)

                    if (!updated) {
                        val response = MessageResponse(success = false, message = "Пользователь не найден")
                        call.respond(HttpStatusCode.NotFound, response)
                        return@put
                    }

                    val response = MessageResponse(success = true, message = "Роль обновлена на ${role.name}")
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    val response = MessageResponse(success = false, message = "Ошибка: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            }

            delete("/users/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@delete
                }

                val deleted = UserRepository.delete(id)

                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, MessageResponse(
                        success = false,
                        message = "Пользователь не найден"
                    ))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, MessageResponse(
                    success = true,
                    message = "Пользователь удалён"
                ))
            }

            get("/tasks") {
                val tasks = TaskRepository.getAll()
                call.respond(HttpStatusCode.OK, TaskListResponse(
                    success = true,
                    message = "Всего задач: ${tasks.size}",
                    data = tasks
                ))
            }

            delete("/tasks/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@delete
                }

                val deleted = TaskRepository.deleteAdmin(id)

                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, MessageResponse(
                        success = false,
                        message = "Задача не найдена"
                    ))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, MessageResponse(
                    success = true,
                    message = "Задача удалена"
                ))
            }
        }
    }
}
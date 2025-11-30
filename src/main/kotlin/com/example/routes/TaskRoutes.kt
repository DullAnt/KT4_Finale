package com.example.routes

import com.example.models.*
import com.example.repository.TaskRepository
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

private fun parseTask(text: String): TaskRequest = json.decodeFromString(text)

fun Route.taskRoutes() {
    authenticate("jwt-auth") {
        route("/api/tasks") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = JwtConfig.getUserId(principal.payload)

                val completed = call.request.queryParameters["completed"]?.toBooleanStrictOrNull()
                val search = call.request.queryParameters["search"]

                val tasks = when {
                    search != null -> TaskRepository.search(userId, search)
                    completed != null -> TaskRepository.filter(userId, completed)
                    else -> TaskRepository.getAllByUser(userId)
                }

                call.respond(HttpStatusCode.OK, TaskListResponse(
                    success = true,
                    message = "Найдено: ${tasks.size}",
                    data = tasks
                ))
            }

            get("{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = JwtConfig.getUserId(principal.payload)
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, TaskResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@get
                }

                val task = TaskRepository.getById(id, userId)

                if (task == null) {
                    call.respond(HttpStatusCode.NotFound, TaskResponse(
                        success = false,
                        message = "Задача не найдена"
                    ))
                    return@get
                }

                call.respond(HttpStatusCode.OK, TaskResponse(
                    success = true,
                    message = "OK",
                    data = task
                ))
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = JwtConfig.getUserId(principal.payload)

                try {
                    val body = call.receiveText()
                    val request = parseTask(body)

                    if (request.title.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, TaskResponse(
                            success = false,
                            message = "Title обязателен"
                        ))
                        return@post
                    }

                    val task = TaskRepository.create(request, userId)

                    call.respond(HttpStatusCode.Created, TaskResponse(
                        success = true,
                        message = "Задача создана",
                        data = task
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, TaskResponse(
                        success = false,
                        message = "Ошибка: ${e.message}"
                    ))
                }
            }

            put("{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = JwtConfig.getUserId(principal.payload)
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, TaskResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@put
                }

                try {
                    val body = call.receiveText()
                    val request = parseTask(body)

                    val task = TaskRepository.update(id, request, userId)

                    if (task == null) {
                        call.respond(HttpStatusCode.NotFound, TaskResponse(
                            success = false,
                            message = "Задача не найдена"
                        ))
                        return@put
                    }

                    call.respond(HttpStatusCode.OK, TaskResponse(
                        success = true,
                        message = "Задача обновлена",
                        data = task
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, TaskResponse(
                        success = false,
                        message = "Ошибка: ${e.message}"
                    ))
                }
            }

            delete("{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = JwtConfig.getUserId(principal.payload)
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, TaskResponse(
                        success = false,
                        message = "Некорректный ID"
                    ))
                    return@delete
                }

                val deleted = TaskRepository.delete(id, userId)

                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, TaskResponse(
                        success = false,
                        message = "Задача не найдена"
                    ))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, TaskResponse(
                    success = true,
                    message = "Задача удалена"
                ))
            }
        }
    }
}
package com.example.models

import kotlinx.serialization.Serializable

// ===== ENUMS =====
enum class Role {
    USER, ADMIN
}

// ===== USER MODELS =====
@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)

@Serializable
data class UserResponse(
    val success: Boolean,
    val message: String,
    val data: User? = null
)

@Serializable
data class UserListResponse(
    val success: Boolean,
    val message: String,
    val data: List<User>? = null
)

@Serializable
data class RoleUpdateRequest(
    val role: String
)

// ===== TASK MODELS =====
@Serializable
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val completed: Boolean = false,
    val userId: Int,
    val createdAt: String
)

@Serializable
data class TaskRequest(
    val title: String,
    val description: String,
    val completed: Boolean = false
)

@Serializable
data class TaskResponse(
    val success: Boolean,
    val message: String,
    val data: Task? = null
)

@Serializable
data class TaskListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Task>? = null
)

// ===== CHAT / WEBSOCKET =====
@Serializable
data class ChatMessage(
    val id: Int = 0,
    val userId: Int,
    val username: String,
    val message: String,
    val timestamp: String
)

@Serializable
data class WsMessage(
    val type: String,  // "chat", "notification", "join", "leave"
    val payload: String
)

// ===== ERROR & INFO =====
@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val code: Int
)

@Serializable
data class ApiInfo(
    val success: Boolean,
    val message: String,
    val version: String,
    val docs: String,
    val websocket: String
)

@Serializable
data class MessageResponse(
    val success: Boolean,
    val message: String
)

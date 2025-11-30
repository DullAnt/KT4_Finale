package com.example.websocket

import com.example.models.ChatMessage
import com.example.models.WsMessage
import com.example.repository.ChatRepository
import com.example.repository.UserRepository
import com.example.security.JwtConfig
import com.auth0.jwt.JWT
import io.ktor.server.application.call
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

data class ChatConnection(
    val session: WebSocketServerSession,
    val userId: Int,
    val username: String
)

object ChatServer {
    private val connections = ConcurrentHashMap<String, ChatConnection>()
    private val json = Json { ignoreUnknownKeys = true }
    
    fun addConnection(sessionId: String, connection: ChatConnection) {
        connections[sessionId] = connection
    }
    
    fun removeConnection(sessionId: String) {
        connections.remove(sessionId)
    }
    
    fun getOnlineUsers(): List<String> {
        return connections.values.map { it.username }.distinct()
    }
    
    suspend fun broadcast(message: WsMessage) {
        val text = json.encodeToString(message)
        connections.values.forEach { conn ->
            try {
                conn.session.send(text)
            } catch (e: Exception) {
                // Игнорируем ошибки отправки
            }
        }
    }
    
    suspend fun broadcastChat(chatMessage: ChatMessage) {
        val wsMessage = WsMessage(
            type = "chat",
            payload = json.encodeToString(chatMessage)
        )
        broadcast(wsMessage)
    }
    
    suspend fun broadcastNotification(text: String) {
        val wsMessage = WsMessage(
            type = "notification",
            payload = text
        )
        broadcast(wsMessage)
    }
}

fun Route.chatWebSocket() {
    webSocket("/ws/chat") {
        // Получаем токен из query параметра
        val token = call.request.queryParameters["token"]
        
        if (token == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token required"))
            return@webSocket
        }
        
        // Проверяем токен
        val payload = try {
            val verifier = JWT.require(JwtConfig.algorithm)
                .withIssuer(JwtConfig.issuer)
                .build()
            verifier.verify(token)
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }
        
        val userId = payload.getClaim("userId").asInt()
        val username = payload.getClaim("username").asString()
        val sessionId = "${userId}_${System.currentTimeMillis()}"
        
        // Добавляем соединение
        val connection = ChatConnection(this, userId, username)
        ChatServer.addConnection(sessionId, connection)
        
        // Отправляем историю чата
        val history = ChatRepository.getRecent(50)
        val historyMessage = WsMessage(
            type = "history",
            payload = Json.encodeToString(history)
        )
        send(Json.encodeToString(historyMessage))
        
        // Уведомляем о входе
        ChatServer.broadcastNotification("$username присоединился к чату")
        
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        
                        // Сохраняем сообщение в БД
                        val chatMessage = ChatRepository.save(userId, text)
                        
                        // Рассылаем всем
                        ChatServer.broadcastChat(chatMessage)
                    }
                    else -> {}
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // Клиент отключился
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ChatServer.removeConnection(sessionId)
            ChatServer.broadcastNotification("$username покинул чат")
        }
    }
    
    // Получить онлайн пользователей
    get("/api/chat/online") {
        val users = ChatServer.getOnlineUsers()
        call.respond(OnlineResponse(
            success = true,
            online = users.size,
            users = users
        ))
    }
}

@kotlinx.serialization.Serializable
data class OnlineResponse(
    val success: Boolean,
    val online: Int,
    val users: List<String>
)

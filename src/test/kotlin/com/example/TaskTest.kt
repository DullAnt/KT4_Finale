package com.example

import com.example.database.DatabaseFactory
import com.example.database.Tasks
import com.example.database.Users
import com.example.models.*
import com.example.plugins.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.*

class TaskTest {

    @BeforeTest
    fun setup() {
        DatabaseFactory.initH2()
        transaction {
            Tasks.deleteAll()
            Users.deleteAll()
        }
    }

    private fun ApplicationTestBuilder.configureApp() {
        application {
            configureSerialization()
            configureAuthentication()
            configureStatusPages()
            configureRouting()
        }
    }

    private suspend fun getToken(client: io.ktor.client.HttpClient): String {
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@mail.com", "1234"))
        }
        return response.body<AuthResponse>().token!!
    }

    @Test
    fun `tasks - unauthorized without token`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val response = client.get("/api/tasks")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `create task - success`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val token = getToken(client)
        
        val response = client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Test Task", "Description"))
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<TaskResponse>()
        assertTrue(body.success)
        assertEquals("Test Task", body.data?.title)
    }

    @Test
    fun `get all tasks`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val token = getToken(client)
        
        client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Task 1", "Desc 1"))
        }
        
        client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Task 2", "Desc 2"))
        }
        
        val response = client.get("/api/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<TaskListResponse>()
        assertEquals(2, body.data?.size)
    }

    @Test
    fun `update task - success`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val token = getToken(client)
        
        val createResponse = client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Original", "Desc"))
        }
        val taskId = createResponse.body<TaskResponse>().data?.id
        
        val response = client.put("/api/tasks/$taskId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Updated", "New Desc", completed = true))
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<TaskResponse>()
        assertEquals("Updated", body.data?.title)
        assertTrue(body.data?.completed == true)
    }

    @Test
    fun `delete task - success`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val token = getToken(client)
        
        val createResponse = client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("To Delete", "Desc"))
        }
        val taskId = createResponse.body<TaskResponse>().data?.id
        
        val deleteResponse = client.delete("/api/tasks/$taskId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, deleteResponse.status)
        
        val getResponse = client.get("/api/tasks/$taskId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `filter by completed`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val token = getToken(client)
        
        client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Task 1", "Desc", completed = false))
        }
        
        client.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(TaskRequest("Task 2", "Desc", completed = true))
        }
        
        val response = client.get("/api/tasks?completed=true") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<TaskListResponse>()
        assertEquals(1, body.data?.size)
        assertTrue(body.data?.first()?.completed == true)
    }
}

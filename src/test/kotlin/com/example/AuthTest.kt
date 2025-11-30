package com.example

import com.example.database.DatabaseFactory
import com.example.models.AuthResponse
import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import com.example.plugins.*
import com.example.repository.UserRepository
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.database.Users
import com.example.database.Tasks
import kotlin.test.*

class AuthTest {

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

    @Test
    fun `register - success`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@mail.com", "1234"))
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body<AuthResponse>()
        assertTrue(body.success)
        assertNotNull(body.token)
        assertEquals("USER", body.user?.role)
    }

    @Test
    fun `register - duplicate username returns 409`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@mail.com", "1234"))
        }
        
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test2@mail.com", "5678"))
        }
        
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `login - success`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@mail.com", "1234"))
        }
        
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("testuser", "1234"))
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertTrue(body.success)
        assertNotNull(body.token)
    }

    @Test
    fun `login - wrong password returns 401`() = testApplication {
        configureApp()
        
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@mail.com", "1234"))
        }
        
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("testuser", "wrong"))
        }
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import java.util.*

object JwtConfig {
    private val SECRET = System.getenv("JWT_SECRET") ?: "ktor-final-secret-key-2024"
    private const val ISSUER = "ktor-final"
    private const val VALIDITY_MS = 3600000L * 24 // 24 hours

    val algorithm: Algorithm = Algorithm.HMAC256(SECRET)
    const val realm = "ktor-final"
    const val issuer = ISSUER

    fun generateToken(userId: Int, username: String, role: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_MS))
            .sign(algorithm)
    }

    fun getUserId(payload: Payload): Int {
        return payload.getClaim("userId").asInt()
    }

    fun getUsername(payload: Payload): String {
        return payload.getClaim("username").asString()
    }

    fun getRole(payload: Payload): String {
        return payload.getClaim("role").asString()
    }
}

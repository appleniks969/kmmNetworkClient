package com.kmm.networkclient.samples

import com.kmm.networkclient.NetworkClient
import com.kmm.networkclient.NetworkClientConfig
import com.kmm.networkclient.NetworkException
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.Serializable

/**
 * Sample data classes for API requests and responses
 */
@Serializable
data class User(val id: Int, val name: String, val email: String)

@Serializable
data class CreateUserRequest(val name: String, val email: String)

@Serializable
data class CreateUserResponse(val id: Int, val createdAt: String)

/**
 * Sample service demonstrating usage of the NetworkClient
 */
class UserService {
    private val baseUrl = "https://api.example.com"
    
    private val client = NetworkClient(
        NetworkClientConfig(
            baseUrl = baseUrl,
            defaultHeaders = mapOf("App-Version" to "1.0.0"),
            enableLogging = true,
            logLevel = LogLevel.HEADERS,
            requestTimeoutMillis = 30000L,
            connectTimeoutMillis = 15000L,
            retryConfig = NetworkClientConfig.RetryConfig(
                maxRetries = 3
            )
        )
    )

    /**
     * Get a user by ID
     */
    suspend fun getUser(id: Int): User {
        return try {
            client.get(
                url = "$baseUrl/users/$id",
                headers = mapOf("Cache-Control" to "max-age=300")
            )
        } catch (e: NetworkException.ClientError) {
            if (e.statusCode == 404) {
                throw IllegalArgumentException("User with ID $id not found")
            } else {
                throw e
            }
        }
    }

    /**
     * Get all users
     */
    suspend fun getUsers(active: Boolean = true): List<User> {
        return client.get(
            url = "$baseUrl/users?active=$active"
        )
    }

    /**
     * Create a new user
     */
    suspend fun createUser(name: String, email: String): CreateUserResponse {
        val request = CreateUserRequest(name, email)
        return client.post(
            url = "$baseUrl/users",
            body = request
        )
    }

    /**
     * Update a user
     */
    suspend fun updateUser(id: Int, name: String?, email: String?): User {
        val updates = mutableMapOf<String, Any>()
        if (name != null) updates["name"] = name
        if (email != null) updates["email"] = email
        
        return client.put(
            url = "$baseUrl/users/$id",
            body = updates
        )
    }

    /**
     * Delete a user
     */
    suspend fun deleteUser(id: Int): Boolean {
        val response: Map<String, Boolean> = client.delete(
            url = "$baseUrl/users/$id"
        )
        return response["success"] ?: false
    }

    /**
     * Close the client when it's no longer needed
     */
    fun close() {
        client.close()
    }
} 
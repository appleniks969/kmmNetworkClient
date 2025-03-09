package com.kmp.network.client.samples

import com.kmm.networkclient.Closeable
import com.kmm.networkclient.NetworkClient
import com.kmm.networkclient.NetworkClientConfig
import com.kmm.networkclient.NetworkException
import io.ktor.client.plugins.logging.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Sample service demonstrating usage of the NetworkClient
 * Implements Closeable to ensure proper resource cleanup
 */
class UserService : Closeable {
    private val baseUrl = "https://jsonplaceholder.typicode.com"
    
    // Custom JSON configuration
    private val customJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        // Add any other custom JSON configuration options here
    }
    
    // Custom headers for all requests
    private val customHeaders = mapOf(
        "App-Version" to "1.0.0",
        "Platform" to "KMM",
        "Accept" to "application/json"
    )
    
    // Create a custom auth config for demonstration
    private val customAuth = NetworkClientConfig.AuthConfig.Custom(
        // Static headers that will be added to every request
        headers = mapOf("X-Api-Key" to "demo-key-12345"),
        // Dynamic headers that can be computed for each request
        authenticator = { dynamicHeaders ->
            // Add timestamp and any other dynamic values
            dynamicHeaders["X-Auth-Timestamp"] = Clock.System.now().toEpochMilliseconds().toString()
            // In a real app, you might compute signatures or add other auth headers
        }
    )
    
    private val client = NetworkClient(
        NetworkClientConfig(
            baseUrl = baseUrl,
            defaultHeaders = customHeaders,
            enableLogging = true,
            logLevel = LogLevel.HEADERS,
            jsonConfiguration = customJson,  // Use our custom JSON config
            requestTimeoutMillis = 30000L,
            connectTimeoutMillis = 15000L,
            authConfig = customAuth,  // Use our custom auth config
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
                url = "/users/$id",
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
    suspend fun getUsers(): List<User> {
        return client.get(
            url = "/users"
        )
    }

    /**
     * Create a new user
     */
    suspend fun createUser(name: String, email: String): CreateUserResponse {
        val request = CreateUserRequest(name, email)
        return client.post(
            url = "/users",
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
            url = "/users/$id",
            body = updates
        )
    }

    /**
     * Delete a user
     */
    suspend fun deleteUser(id: Int): Boolean {
        val response: Map<String, Boolean> = client.delete(
            url = "/users/$id"
        )
        return response["success"] ?: false
    }

    /**
     * Close the client when it's no longer needed
     * Implementation of the Closeable interface
     */
    override fun close() {
        client.close()
    }
} 
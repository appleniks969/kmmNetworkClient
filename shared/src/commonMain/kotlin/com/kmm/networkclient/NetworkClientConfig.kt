package com.kmm.networkclient

import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Configuration class for NetworkClient.
 */
data class NetworkClientConfig(
    /**
     * The base URL for all requests.
     */
    val baseUrl: String? = null,
    
    /**
     * Default headers to be included in all requests.
     */
    val defaultHeaders: Map<String, String> = emptyMap(),
    
    /**
     * Whether to expect successful responses (status code 2xx).
     */
    val expectSuccess: Boolean = true,
    
    /**
     * Whether to enable logging.
     */
    val enableLogging: Boolean = false,
    
    /**
     * The logging level.
     */
    val logLevel: LogLevel = LogLevel.HEADERS,
    
    /**
     * Json configuration for serialization/deserialization.
     * If null, a default configuration will be used.
     */
    val jsonConfiguration: Json? = null,
    
    /**
     * Whether the JSON parser should be lenient.
     * Only used if jsonConfiguration is null.
     */
    val isLenient: Boolean = true,
    
    /**
     * Whether to ignore unknown keys in JSON responses.
     * Only used if jsonConfiguration is null.
     */
    val ignoreUnknownKeys: Boolean = true,
    
    /**
     * Whether to use pretty print for JSON.
     * Only used if jsonConfiguration is null.
     */
    val prettyPrint: Boolean = true
) {
    /**
     * Helper method to create a Json instance based on configuration.
     * Uses provided jsonConfiguration if not null, otherwise creates a new instance
     * with the specified parameters.
     */
    fun createJson(): Json {
        return jsonConfiguration ?: Json {
            prettyPrint = this@NetworkClientConfig.prettyPrint
            isLenient = this@NetworkClientConfig.isLenient
            ignoreUnknownKeys = this@NetworkClientConfig.ignoreUnknownKeys
        }
    }
} 
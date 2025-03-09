package com.kmm.networkclient

import io.ktor.client.plugins.logging.*
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
    val prettyPrint: Boolean = true,
    
    /**
     * Authentication configuration.
     */
    val authConfig: AuthConfig? = null,
    
    /**
     * Request timeout in milliseconds.
     */
    val requestTimeoutMillis: Long = 30000L,
    
    /**
     * Connection timeout in milliseconds.
     */
    val connectTimeoutMillis: Long = 15000L,
    
    /**
     * Socket timeout in milliseconds.
     */
    val socketTimeoutMillis: Long = 30000L,
    
    /**
     * Retry configuration.
     */
    val retryConfig: RetryConfig = RetryConfig()
) {
    /**
     * Authentication configuration
     */
    sealed class AuthConfig {
        /**
         * Bearer token authentication
         */
        data class Bearer(
            val getToken: () -> String,
            val refreshToken: String? = null,
            val customHeaders: Map<String, String> = emptyMap()
        ) : AuthConfig()
        
        /**
         * Basic authentication
         */
        data class Basic(
            val username: String,
            val password: String,
            val customHeaders: Map<String, String> = emptyMap()
        ) : AuthConfig()
        
        /**
         * Custom authentication
         * This allows clients to define their own authentication mechanism
         * 
         * @param headers Static headers to include in every request
         * @param authenticator Function that provides dynamic headers for each request
         */
        data class Custom(
            val headers: Map<String, String> = emptyMap(),
            val authenticator: (dynamicHeaders: MutableMap<String, String>) -> Unit
        ) : AuthConfig()
    }
    
    /**
     * Retry configuration
     */
    data class RetryConfig(
        /**
         * Maximum number of retries.
         */
        val maxRetries: Int = 0,
        
        /**
         * Base value for exponential backoff.
         */
        val exponentialBase: Double = 2.0,
        
        /**
         * Maximum delay in milliseconds.
         */
        val maxDelayMs: Long = 3000L
    )
    
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
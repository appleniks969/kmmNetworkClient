package com.kmm.networkclient

import io.ktor.client.plugins.logging.*

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
     * Whether the JSON parser should be lenient.
     */
    val isLenient: Boolean = true,
    
    /**
     * Whether to ignore unknown keys in JSON responses.
     */
    val ignoreUnknownKeys: Boolean = true,
    
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
            val refreshToken: String? = null
        ) : AuthConfig()
        
        /**
         * Basic authentication
         */
        data class Basic(
            val username: String,
            val password: String
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
} 
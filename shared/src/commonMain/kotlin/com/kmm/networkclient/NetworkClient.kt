package com.kmm.networkclient

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A KMM network client that provides common functionality for making HTTP requests.
 * This client is configured with default settings that can be overridden.
 * Implements Closeable to ensure proper resource cleanup.
 */
class NetworkClient(
    private val config: NetworkClientConfig = NetworkClientConfig()
) : Closeable {
    
    val httpClient = createHttpClient(config)
    
    companion object {
        /**
         * Creates an HTTP client with the provided configuration.
         */
        private fun createHttpClient(config: NetworkClientConfig): HttpClient {
            return HttpClient {
                expectSuccess = config.expectSuccess
                
                // Configure logging
                if (config.enableLogging) {
                    install(Logging) {
                        logger = Logger.DEFAULT
                        level = config.logLevel
                    }
                }
                
                // Configure content negotiation
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = config.isLenient
                        ignoreUnknownKeys = config.ignoreUnknownKeys
                    })
                }
                
                // Configure default request settings
                defaultRequest {
                    config.defaultHeaders.forEach { (key, value) ->
                        header(key, value)
                    }
                    
                    config.baseUrl?.let {
                        url(it)
                    }
                }
                
                // Configure authentication if needed
                if (config.authConfig != null) {
                    install(Auth) {
                        when (val authConfig = config.authConfig) {
                            is NetworkClientConfig.AuthConfig.Bearer -> {
                                bearer {
                                    loadTokens {
                                        BearerTokens(
                                            accessToken = authConfig.getToken(),
                                            refreshToken = authConfig.refreshToken ?: ""
                                        )
                                    }
                                    
                                    refreshTokens {
                                        authConfig.refreshToken?.let {
                                            BearerTokens(
                                                accessToken = authConfig.getToken(),
                                                refreshToken = it
                                            )
                                        } ?: BearerTokens(
                                            accessToken = authConfig.getToken(),
                                            refreshToken = ""
                                        )
                                    }
                                }
                            }
                            is NetworkClientConfig.AuthConfig.Basic -> {
                                basic {
                                    credentials {
                                        BasicAuthCredentials(
                                            username = authConfig.username,
                                            password = authConfig.password
                                        )
                                    }
                                }
                            }
                            else -> {
                                // No additional configuration for other auth types
                            }
                        }
                    }
                }

                // Configure timeout
                install(HttpTimeout) {
                    requestTimeoutMillis = config.requestTimeoutMillis
                    connectTimeoutMillis = config.connectTimeoutMillis
                    socketTimeoutMillis = config.socketTimeoutMillis
                }
                
                // Configure HttpRequestRetry
                if (config.retryConfig.maxRetries > 0) {
                    install(HttpRequestRetry) {
                        retryOnExceptionOrServerErrors(
                            maxRetries = config.retryConfig.maxRetries
                        )
                        exponentialDelay(
                            base = config.retryConfig.exponentialBase,
                            maxDelayMs = config.retryConfig.maxDelayMs
                        )
                    }
                }
            }
        }
    }

    /**
     * Makes a GET request to the specified URL and returns the response as the specified type.
     * 
     * @param url The URL to make the request to
     * @param headers Optional headers to include in the request
     * @return The response body as the specified type
     */
    suspend inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleRequest {
            httpClient.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
        }
    }

    /**
     * Makes a POST request to the specified URL with the specified body and returns the response as the specified type.
     * 
     * @param url The URL to make the request to
     * @param body The body of the request
     * @param headers Optional headers to include in the request
     * @return The response body as the specified type
     */
    suspend inline fun <reified T, reified R> post(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): R {
        return handleRequest {
            httpClient.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }
    }

    /**
     * Makes a PUT request to the specified URL with the specified body and returns the response as the specified type.
     * 
     * @param url The URL to make the request to
     * @param body The body of the request
     * @param headers Optional headers to include in the request
     * @return The response body as the specified type
     */
    suspend inline fun <reified T, reified R> put(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): R {
        return handleRequest {
            httpClient.put(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }
    }

    /**
     * Makes a DELETE request to the specified URL and returns the response as the specified type.
     * 
     * @param url The URL to make the request to
     * @param headers Optional headers to include in the request
     * @return The response body as the specified type
     */
    suspend inline fun <reified T> delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleRequest {
            httpClient.delete(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
        }
    }

    /**
     * Makes a PATCH request to the specified URL with the specified body and returns the response as the specified type.
     * 
     * @param url The URL to make the request to
     * @param body The body of the request
     * @param headers Optional headers to include in the request
     * @return The response body as the specified type
     */
    suspend inline fun <reified T, reified R> patch(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): R {
        return handleRequest {
            httpClient.patch(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }
    }

    /**
     * Helper function to handle exceptions and retry logic for network requests.
     */
    suspend fun <T> handleRequest(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: ClientRequestException) {
            throw NetworkException.ClientError(
                message = e.message ?: "Client error",
                statusCode = e.response.status.value,
                response = e.response.bodyAsText()
            )
        } catch (e: ServerResponseException) {
            throw NetworkException.ServerError(
                message = e.message ?: "Server error",
                statusCode = e.response.status.value,
                response = e.response.bodyAsText()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw NetworkException.UnknownError(
                message = e.message ?: "Unknown error",
                cause = e
            )
        }
    }

    /**
     * Closes the HTTP client and releases all resources.
     * Implementation of the Closeable interface.
     */
    override fun close() {
        httpClient.close()
    }
}

/**
 * Interface for closeable resources
 */
interface Closeable {
    fun close()
} 
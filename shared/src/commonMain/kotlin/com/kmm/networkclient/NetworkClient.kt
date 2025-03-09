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
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

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
                
                // Configure content negotiation with custom JSON if provided
                install(ContentNegotiation) {
                    json(config.createJson())
                }
                
                // Configure default request settings with all default headers
                defaultRequest {
                    config.defaultHeaders.forEach { (key, value) ->
                        header(key, value)
                    }
                    
                    // Apply any auth-related headers that should be included in every request
                    if (config.authConfig != null) {
                        when (val authConfig = config.authConfig) {
                            is NetworkClientConfig.AuthConfig.Custom -> {
                                // Apply custom auth headers
                                authConfig.headers.forEach { (key, value) ->
                                    header(key, value)
                                }
                                
                                // Allow the authenticator callback to add dynamic headers for each request
                                val dynamicHeaders = mutableMapOf<String, String>()
                                authConfig.authenticator(dynamicHeaders)
                                dynamicHeaders.forEach { (key, value) ->
                                    header(key, value)
                                }
                            }
                            is NetworkClientConfig.AuthConfig.Bearer -> {
                                // Add custom headers for Bearer auth
                                authConfig.customHeaders.forEach { (key, value) ->
                                    header(key, value)
                                }
                                // The actual token is handled by the Auth plugin
                            }
                            is NetworkClientConfig.AuthConfig.Basic -> {
                                // Add custom headers for Basic auth
                                authConfig.customHeaders.forEach { (key, value) ->
                                    header(key, value)
                                }
                                // The actual credentials are handled by the Auth plugin
                            }
                            is NetworkClientConfig.AuthConfig.Dynamic, 
                            is NetworkClientConfig.AuthConfig.RuleBased,
                            is NetworkClientConfig.AuthConfig.NoAuth -> {
                                // Dynamic auth is handled per-request based on method and path
                                // NoAuth doesn't add any headers at the global level
                                // We'll just apply common headers here if needed
                            }
                            else -> {}
                        }
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
                            // Dynamic auth types are applied in request pipeline
                            else -> {}
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
     * Applies dynamic authentication based on HTTP method and request path
     */
    public fun HttpRequestBuilder.applyDynamicAuth(method: HttpMethod, path: String) {
        if (config.authConfig == null) return
        
        val effectiveAuthConfig = when (val authConfig = config.authConfig) {
            is NetworkClientConfig.AuthConfig.Dynamic -> {
                // Use the selector function to determine the authentication method
                authConfig.selector(method, path)
            }
            is NetworkClientConfig.AuthConfig.RuleBased -> {
                // Find a matching rule based on method and path
                val matchingRule = authConfig.rules.firstOrNull { rule ->
                    (rule.methods == null || method in rule.methods) && 
                    rule.pathPattern.matches(path)
                }
                
                // Use the matching rule's auth config or fall back to default
                matchingRule?.authConfig ?: authConfig.defaultConfig
            }
            // If not using dynamic auth, just use the configured auth
            else -> authConfig
        }
        
        // Apply the effective authentication configuration
        when (val auth = effectiveAuthConfig) {
            is NetworkClientConfig.AuthConfig.Custom -> {
                // Apply custom auth headers
                auth.headers.forEach { (key, value) ->
                    header(key, value)
                }
                
                // Add dynamic headers
                val dynamicHeaders = mutableMapOf<String, String>()
                auth.authenticator(dynamicHeaders)
                dynamicHeaders.forEach { (key, value) ->
                    header(key, value)
                }
            }
            is NetworkClientConfig.AuthConfig.Bearer -> {
                // Add the bearer token header
                header(HttpHeaders.Authorization, "Bearer ${auth.getToken()}")
                
                // Add any custom headers
                auth.customHeaders.forEach { (key, value) ->
                    header(key, value)
                }
            }
            is NetworkClientConfig.AuthConfig.Basic -> {
                // Instead of manually encoding, let Ktor handle this by setting up basic auth
                // for this specific request
                basicAuth(auth.username, auth.password)
                
                // Add any custom headers
                auth.customHeaders.forEach { (key, value) ->
                    header(key, value)
                }
            }
            is NetworkClientConfig.AuthConfig.NoAuth -> {
                // Explicitly no authentication - do nothing
                // This case is handled explicitly to make it clear that authentication is intentionally skipped
            }
            null -> {
                // Null means no authentication for this request
                // This can happen when using Dynamic authentication and the selector returns null
            }
            else -> {
                // For nested dynamic auth types, we don't apply anything
                // to avoid potential infinite recursion
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
                
                // Extract path from URL for dynamic auth
                val path = extractPathFromUrl(url)
                applyDynamicAuth(HttpMethod.Get, path)
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
                
                // Extract path from URL for dynamic auth
                val path = extractPathFromUrl(url)
                applyDynamicAuth(HttpMethod.Post, path)
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
                
                // Extract path from URL for dynamic auth
                val path = extractPathFromUrl(url)
                applyDynamicAuth(HttpMethod.Put, path)
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
                
                // Extract path from URL for dynamic auth
                val path = extractPathFromUrl(url)
                applyDynamicAuth(HttpMethod.Delete, path)
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
                
                // Extract path from URL for dynamic auth
                val path = extractPathFromUrl(url)
                applyDynamicAuth(HttpMethod.Patch, path)
            }.body()
        }
    }
    
    /**
     * Helper method to extract the path from a URL
     */
    public fun extractPathFromUrl(url: String): String {
        // Try to parse the URL
        return try {
            val urlObj = Url(url)
            urlObj.encodedPath
        } catch (e: Exception) {
            // If the URL is a relative path (not a complete URL)
            if (!url.startsWith("http")) {
                url.substringBefore('?')
            } else {
                // For malformed URLs, just use the whole string
                url
            }
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
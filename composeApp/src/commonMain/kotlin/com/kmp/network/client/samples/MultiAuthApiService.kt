package com.kmp.network.client.samples

import com.kmm.networkclient.Closeable
import com.kmm.networkclient.NetworkClient
import com.kmm.networkclient.NetworkClientConfig
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/**
 * Example service demonstrating the use of dynamic authentication for different APIs and HTTP methods.
 * 
 * This sample shows:
 * 1. Different authentication for GET vs POST on the same endpoint
 * 2. Different authentication for different API endpoints
 * 3. Rule-based authentication patterns
 * 4. Explicitly skipping authentication for public endpoints
 */
class MultiAuthApiService : Closeable {
    // Create different authentication configurations
    
    // Bearer token authentication
    private val bearerAuth = NetworkClientConfig.AuthConfig.Bearer(
        getToken = { "bearer-token-12345" },
        customHeaders = mapOf("Authorization-Type" to "Bearer")
    )
    
    // Basic authentication
    private val basicAuth = NetworkClientConfig.AuthConfig.Basic(
        username = "apiuser",
        password = "apisecret",
        customHeaders = mapOf("Authorization-Type" to "Basic")
    )
    
    // API key authentication
    private val apiKeyAuth = NetworkClientConfig.AuthConfig.Custom(
        headers = mapOf("X-Api-Key" to "api-key-12345"),
        authenticator = { dynamicHeaders ->
            dynamicHeaders["X-Request-Timestamp"] = Clock.System.now().toEpochMilliseconds().toString()
        }
    )
    
    // Signature-based authentication
    private val signatureAuth = NetworkClientConfig.AuthConfig.Custom(
        headers = mapOf("X-Client-Id" to "client-12345"),
        authenticator = { dynamicHeaders ->
            val timestamp = Clock.System.now().toEpochMilliseconds().toString()
            dynamicHeaders["X-Timestamp"] = timestamp
            
            // In a real app, you would compute a signature based on request details
            val fakeSignature = "signature-${timestamp.takeLast(4)}"
            dynamicHeaders["X-Signature"] = fakeSignature
        }
    )
    
    // Example 1: Different auth for GET vs POST on same endpoint
    private val methodBasedClient = NetworkClient(
        NetworkClientConfig(
            baseUrl = "https://api.example.com",
            enableLogging = true,
            logLevel = LogLevel.BODY,
            
            // Use dynamic authentication based on HTTP method
            authConfig = NetworkClientConfig.AuthConfig.Dynamic { method, path ->
                when (method) {
                    HttpMethod.Get -> apiKeyAuth     // Simple API key for GET requests
                    HttpMethod.Post -> signatureAuth // More secure signature auth for POST
                    else -> basicAuth                // Default to basic auth for other methods
                }
            }
        )
    )
    
    // Example 2: Different auth based on API endpoint patterns using rules
    private val endpointBasedClient = NetworkClient(
        NetworkClientConfig(
            baseUrl = "https://api.multiservice.com",
            enableLogging = true,
            logLevel = LogLevel.HEADERS,
            
            // Use rule-based authentication
            authConfig = NetworkClientConfig.AuthConfig.RuleBased(
                rules = listOf(
                    // Public API endpoints use API key auth
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/public/.*",
                        authConfig = apiKeyAuth
                    ),
                    
                    // User data endpoints use bearer token
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/users/.*",
                        authConfig = bearerAuth
                    ),
                    
                    // Admin endpoints use signature auth for POST/PUT/DELETE but basic auth for GET
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        methods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete),
                        pathPattern = "/admin/.*",
                        authConfig = signatureAuth
                    ),
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        methods = setOf(HttpMethod.Get),
                        pathPattern = "/admin/.*",
                        authConfig = basicAuth
                    )
                ),
                // Default to API key auth if no rules match
                defaultConfig = apiKeyAuth
            )
        )
    )
    
    // Example 3: Different auth for completely different API services
    private val multiServiceClient = NetworkClient(
        NetworkClientConfig(
            // No baseUrl - we'll specify full URLs
            enableLogging = true,
            logLevel = LogLevel.INFO,
            
            // Use dynamic authentication based on domain/path
            authConfig = NetworkClientConfig.AuthConfig.Dynamic { _, url ->
                when {
                    // Finance API requires signature auth
                    url.contains("/finance/") -> signatureAuth
                    
                    // Analytics API uses basic auth
                    url.contains("/analytics/") -> basicAuth
                    
                    // User API uses bearer token
                    url.contains("/user/") -> bearerAuth
                    
                    // Default to API key auth
                    else -> apiKeyAuth
                }
            }
        )
    )
    
    // Example 4: Client with mixed auth and no-auth endpoints
    private val mixedAuthClient = NetworkClient(
        NetworkClientConfig(
            baseUrl = "https://api.mixed-auth.com",
            enableLogging = true,
            logLevel = LogLevel.HEADERS,
            
            // Use rule-based authentication with NoAuth for public endpoints
            authConfig = NetworkClientConfig.AuthConfig.RuleBased(
                rules = listOf(
                    // Public documentation endpoints - no auth needed
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/docs/.*",
                        authConfig = NetworkClientConfig.AuthConfig.NoAuth
                    ),
                    
                    // Open API endpoints - no auth needed
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/open-api/.*",
                        authConfig = NetworkClientConfig.AuthConfig.NoAuth
                    ),
                    
                    // Health check endpoint - no auth
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/health",
                        authConfig = NetworkClientConfig.AuthConfig.NoAuth
                    ),
                    
                    // User data requires authentication
                    NetworkClientConfig.AuthConfig.RuleBased.rule(
                        pathPattern = "/users/.*",
                        authConfig = bearerAuth
                    )
                ),
                // Default requires API key auth
                defaultConfig = apiKeyAuth
            )
        )
    )
    
    // Example 5: Dynamic auth with some paths explicitly skipped
    private val selectiveAuthClient = NetworkClient(
        NetworkClientConfig(
            baseUrl = "https://api.selective-auth.com",
            enableLogging = true,
            
            // Use dynamic authentication that returns null or NoAuth for public endpoints
            authConfig = NetworkClientConfig.AuthConfig.Dynamic { method, path ->
                // Skip authentication for public resources and OPTIONS requests
                if (method == HttpMethod.Options || path.startsWith("/public/")) {
                    NetworkClientConfig.AuthConfig.NoAuth
                } else {
                    // Apply different auth based on path and method
                    when {
                        path.startsWith("/admin/") -> signatureAuth
                        path.startsWith("/user/") -> bearerAuth
                        else -> apiKeyAuth
                    }
                }
            }
        )
    )
    
    // Data classes for API responses
    @Serializable
    data class PublicData(val id: String, val name: String, val description: String)
    
    @Serializable
    data class UserData(val userId: String, val email: String, val profile: UserProfile)
    
    @Serializable
    data class UserProfile(val fullName: String, val pictureUrl: String)
    
    @Serializable
    data class AdminData(val id: String, val restricted: Boolean, val permissions: List<String>)
    
    // Sample methods demonstrating the usage
    
    // Example 1: Method-based auth
    suspend fun getPublicData(id: String): PublicData {
        return methodBasedClient.get("/api/public/$id")
    }
    
    suspend fun createPublicData(data: PublicData): PublicData {
        return methodBasedClient.post("/api/public", data)
    }
    
    // Example 2: Endpoint-based auth
    suspend fun getPublicResource(id: String): PublicData {
        return endpointBasedClient.get("/public/resources/$id")
    }
    
    suspend fun getUserData(userId: String): UserData {
        return endpointBasedClient.get("/users/$userId")
    }
    
    suspend fun getAdminData(id: String): AdminData {
        return endpointBasedClient.get("/admin/data/$id") // Uses basic auth due to rule
    }
    
    suspend fun updateAdminData(data: AdminData): AdminData {
        return endpointBasedClient.put("/admin/data/${data.id}", data) // Uses signature auth due to rule
    }
    
    // Example 3: Multi-service auth
    suspend fun getFinanceData(accountId: String): Map<String, Double> {
        return multiServiceClient.get("https://finance-api.example.com/finance/accounts/$accountId")
    }
    
    suspend fun getAnalyticsData(metric: String): Map<String, Int> {
        return multiServiceClient.get("https://analytics-api.example.com/analytics/metrics/$metric")
    }
    
    suspend fun getUserProfile(userId: String): UserProfile {
        return multiServiceClient.get("https://user-api.example.com/user/profiles/$userId")
    }
    
    // Example 4: Mixed auth/no-auth endpoints
    suspend fun getApiDocumentation(path: String): String {
        return mixedAuthClient.get("/docs/$path") // No authentication needed
    }
    
    suspend fun getHealthStatus(): Map<String, String> {
        return mixedAuthClient.get("/health") // No authentication needed
    }
    
    suspend fun getOpenApiData(id: String): PublicData {
        return mixedAuthClient.get("/open-api/data/$id") // No authentication needed
    }
    
    suspend fun getUserDataWithMixedClient(userId: String): UserData {
        return mixedAuthClient.get("/users/$userId") // Uses bearer token authentication
    }
    
    // Example 5: Selective auth based on method and path
    suspend fun getPublicDataWithSelectiveAuth(id: String): PublicData {
        return selectiveAuthClient.get("/public/data/$id") // No authentication
    }
    
    suspend fun getUserWithSelectiveAuth(userId: String): UserData {
        return selectiveAuthClient.get("/user/$userId") // Bearer token auth
    }
    
    suspend fun getOptionsForEndpoint(path: String): Map<String, List<String>> {
        return selectiveAuthClient.get("/api$path") // No auth for OPTIONS requests
    }
    
    override fun close() {
        methodBasedClient.close()
        endpointBasedClient.close()
        multiServiceClient.close()
        mixedAuthClient.close()
        selectiveAuthClient.close()
    }
} 
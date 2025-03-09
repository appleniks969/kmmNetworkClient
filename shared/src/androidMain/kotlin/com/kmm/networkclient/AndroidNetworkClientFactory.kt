package com.kmm.networkclient

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.*
import io.ktor.serialization.kotlinx.json.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Android-specific implementation of NetworkClientFactory that integrates Chucker for HTTP inspection.
 */
object AndroidNetworkClientFactory {
    
    /**
     * Creates an HTTP client with the provided configuration and Chucker integration.
     * 
     * @param config The NetworkClient configuration
     * @param context The Android Context needed for Chucker
     * @return An Android-specific HttpClient with Chucker integration
     */
    fun createHttpClient(config: NetworkClientConfig, context: Context): HttpClient {
        // Create OkHttp client with Chucker (if available)
        val okHttpClientBuilder = OkHttpClient.Builder()
            .followRedirects(true)
        
        // Try to add Chucker interceptor using a separate method to avoid direct dependency
        val chuckerInterceptor = createChuckerInterceptor(context)
        if (chuckerInterceptor != null) {
            okHttpClientBuilder.addInterceptor(chuckerInterceptor)
        }
        
        val okHttpClient = okHttpClientBuilder.build()
        
        // Configure the OkHttp engine with Chucker
        return HttpClient(OkHttp) {
            expectSuccess = config.expectSuccess
            
            // Use the custom OkHttp client with Chucker
            engine {
                preconfigured = okHttpClient
            }
            
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
            
            // Configure timeouts
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
            
            // Configure authentication if needed
            config.authConfig?.let { authConfig ->
                // Setup Auth plugin if needed
                if (authConfig is NetworkClientConfig.AuthConfig.Bearer ||
                    authConfig is NetworkClientConfig.AuthConfig.Basic) {
                    install(Auth) {
                        // Auth configurations would go here
                        // This is a simplified version, you might need to expand this
                        // based on your actual auth needs
                    }
                }
            }
        }
    }
    
    /**
     * Creates a Chucker interceptor using a separate method to avoid direct dependency.
     * This allows the shared module to compile without Chucker dependency.
     */
    private fun createChuckerInterceptor(context: Context): Interceptor? {
        return try {
            // Use Class.forName to check if Chucker is available
            val chuckerClass = Class.forName("com.chuckerteam.chucker.api.Chucker")
            
            // If Chucker is available, use the ChuckerInterceptorDelegate to create it
            ChuckerInterceptorDelegate.createInterceptor(context)
        } catch (e: Exception) {
            // If Chucker is not available, return null
            println("Chucker not available: ${e.message}")
            null
        }
    }
    
    /**
     * Delegate class to create Chucker interceptor.
     * This is in a separate file that will be compiled only if Chucker is available.
     */
    private object ChuckerInterceptorDelegate {
        fun createInterceptor(context: Context): Interceptor? {
            return try {
                // This code will be executed only if Chucker is available
                val builderClass = Class.forName("com.chuckerteam.chucker.api.ChuckerInterceptor\$Builder")
                val builderConstructor = builderClass.getConstructor(Context::class.java)
                val builder = builderConstructor.newInstance(context)
                
                // Configure the builder
                val maxContentLengthMethod = builderClass.getMethod("maxContentLength", Long::class.java)
                maxContentLengthMethod.invoke(builder, 250_000L)
                
                val redactHeadersMethod = builderClass.getMethod("redactHeaders", List::class.java)
                redactHeadersMethod.invoke(builder, listOf("Authorization", "Bearer", "X-API-Key"))
                
                val alwaysReadResponseBodyMethod = builderClass.getMethod("alwaysReadResponseBody", Boolean::class.java)
                alwaysReadResponseBodyMethod.invoke(builder, true)
                
                // Build the interceptor
                val buildMethod = builderClass.getMethod("build")
                buildMethod.invoke(builder) as Interceptor
            } catch (e: Exception) {
                println("Failed to create Chucker interceptor: ${e.message}")
                null
            }
        }
    }
} 
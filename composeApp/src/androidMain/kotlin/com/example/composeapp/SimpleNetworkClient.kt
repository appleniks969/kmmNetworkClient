package com.example.composeapp

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * A simple network client implementation for the Android app.
 */
class SimpleNetworkClient(
    private val baseUrl: String,
    private val context: Context? = null
) {
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    
    private val httpClient = HttpClient(OkHttp) {
        // Configure the client
        engine {
            // Configure OkHttp
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                
                // Add Chucker interceptor if context is provided
                context?.let { ctx ->
                    val chuckerCollector = ChuckerCollector(
                        context = ctx,
                        showNotification = true,
                        retentionPeriod = RetentionManager.Period.ONE_HOUR
                    )
                    
                    val chuckerInterceptor = ChuckerInterceptor.Builder(ctx)
                        .collector(chuckerCollector)
                        .maxContentLength(250_000L)
                        .redactHeaders(emptySet())
                        .alwaysReadResponseBody(true)
                        .build()
                    
                    addInterceptor(chuckerInterceptor)
                }
            }
        }
        
        expectSuccess = true
        
        // Add logging
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        
        // Add content negotiation for JSON
        install(ContentNegotiation) {
            json(json)
        }
    }
    
    /**
     * Performs a GET request and returns the response as a string.
     */
    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                appendPathSegments(path)
            }
        }
        
        response.bodyAsText()
    }
    
    /**
     * Closes the HTTP client to free resources.
     */
    suspend fun close() {
        httpClient.close()
    }
} 
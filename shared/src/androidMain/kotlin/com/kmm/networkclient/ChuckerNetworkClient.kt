package com.kmm.networkclient

import android.content.Context
import android.content.Intent
import com.chuckerteam.chucker.api.Chucker
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

/**
 * A utility class that provides Chucker integration for the NetworkClient.
 * This class is only available on Android.
 */
object ChuckerNetworkClient {
    
    /**
     * Creates a NetworkClient with Chucker integration.
     * 
     * @param config The configuration for the NetworkClient
     * @param context The Android context
     * @return A NetworkClient with Chucker integration
     */
    fun create(
        config: NetworkClientConfig = NetworkClientConfig(),
        context: Context
    ): NetworkClient {
        // Store the context for later use
        AndroidContextProvider.initialize(context)
        
        // Create OkHttp client with Chucker interceptor
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .addInterceptor(createChuckerInterceptor(context))
            .build()
        
        // Create HTTP client with the OkHttp engine
        val httpClient = HttpClient(OkHttp) {
            expectSuccess = config.expectSuccess
            
            // Use the custom OkHttp client with Chucker
            engine {
                preconfigured = okHttpClient
            }
            
            // Apply the rest of the configuration
            config.apply(this)
        }
        
        // Return a NetworkClient with the custom HTTP client
        return NetworkClient(config, httpClient)
    }
    
    /**
     * Opens the Chucker UI to inspect network traffic.
     * 
     * @param context The Android context
     */
    fun openChuckerScreen(context: Context) {
        context.startActivity(Chucker.getLaunchIntent(context))
    }
    
    /**
     * Creates a Chucker interceptor for network monitoring.
     * 
     * @param context The Android context
     * @return A configured Chucker interceptor
     */
    private fun createChuckerInterceptor(context: Context): ChuckerInterceptor {
        // Create a collector with a 1-hour retention period
        val chuckerCollector = ChuckerCollector(
            context = context,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
        
        // Create and configure the Chucker interceptor
        return ChuckerInterceptor.Builder(context)
            .collector(chuckerCollector)
            .maxContentLength(250_000L)
            .redactHeaders(listOf("Authorization", "Bearer", "X-API-Key"))
            .alwaysReadResponseBody(true)
            .build()
    }
    
    /**
     * Returns an Intent to launch the Chucker UI.
     * 
     * @param context The Android context
     * @return An Intent to launch the Chucker UI
     */
    fun getLaunchIntent(context: Context): Intent {
        return Chucker.getLaunchIntent(context)
    }
} 
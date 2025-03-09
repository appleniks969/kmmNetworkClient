package com.kmm.networkclient

import android.content.Context

/**
 * Android-specific extension functions for NetworkClient.
 */

/**
 * Creates a NetworkClient with Chucker integration.
 * 
 * @param config The configuration for the NetworkClient
 * @param context The Android context for Chucker
 * @return A NetworkClient with Chucker integration
 */
fun createNetworkClientWithChucker(
    config: NetworkClientConfig = NetworkClientConfig(),
    context: Context
): NetworkClient {
    // Create a custom HTTP client with Chucker integration
    val httpClient = AndroidNetworkClientFactory.createHttpClient(config, context)
    
    // Return a NetworkClient that uses this custom HTTP client
    return NetworkClient(config, httpClient)
}

/**
 * Creates a NetworkClient with Chucker integration using the cached application context.
 * The AndroidContextProvider must be initialized before calling this function.
 * 
 * @param config The configuration for the NetworkClient
 * @return A NetworkClient with Chucker integration or a regular NetworkClient if context is not available
 */
fun createNetworkClient(config: NetworkClientConfig = NetworkClientConfig()): NetworkClient {
    val context = AndroidContextProvider.getApplicationContext()
    
    return if (context != null) {
        // Use Chucker if we have a context
        createNetworkClientWithChucker(config, context)
    } else {
        // Fall back to regular NetworkClient if no context is available
        NetworkClient(config)
    }
}
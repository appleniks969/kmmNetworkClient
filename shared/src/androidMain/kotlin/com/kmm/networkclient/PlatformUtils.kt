package com.kmm.networkclient

import android.content.Context

/**
 * Android-specific implementation of platform utilities
 */

/**
 * Create a platform-optimized NetworkClient for Android.
 * This implementation will try to use Chucker integration if available.
 */
actual fun createPlatformNetworkClient(config: NetworkClientConfig): NetworkClient {
    val context = AndroidContextProvider.getApplicationContext()
    
    return if (context != null) {
        // Use Chucker if we have a context
        ChuckerNetworkClient.create(config, context)
    } else {
        // Fall back to regular NetworkClient if no context is available
        NetworkClient(config)
    }
} 
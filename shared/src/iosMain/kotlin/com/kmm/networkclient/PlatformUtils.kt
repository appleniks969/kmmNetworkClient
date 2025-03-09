package com.kmm.networkclient

/**
 * iOS-specific implementation of platform utilities
 */

/**
 * Create a platform-optimized NetworkClient for iOS.
 * This implementation just returns a standard NetworkClient since
 * Chucker is not available on iOS.
 */
actual fun createPlatformNetworkClient(config: NetworkClientConfig): NetworkClient {
    return NetworkClient(config)
} 
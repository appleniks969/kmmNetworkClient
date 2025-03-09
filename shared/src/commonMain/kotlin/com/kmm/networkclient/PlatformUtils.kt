package com.kmm.networkclient

/**
 * Platform-specific extensions for NetworkClient
 */

/**
 * Create a platform-optimized NetworkClient.
 * On Android, this will create a client with Chucker integration if available.
 * On other platforms, it will create a standard NetworkClient.
 */
expect fun createPlatformNetworkClient(config: NetworkClientConfig = NetworkClientConfig()): NetworkClient 
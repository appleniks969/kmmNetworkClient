# KMM Network Client

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-blue.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A powerful and flexible Kotlin Multiplatform Mobile (KMM) network client library built on top of Ktor, enabling shared networking code between Android and iOS applications.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Basic Usage](#basic-usage)
- [Authentication Options](#authentication-options)
  - [Basic Authentication](#basic-authentication)
  - [Bearer Token Authentication](#bearer-token-authentication)
  - [Custom Authentication](#custom-authentication)
  - [Dynamic Authentication](#dynamic-authentication)
  - [Rule-Based Authentication](#rule-based-authentication)
  - [No Authentication](#no-authentication)
- [Error Handling](#error-handling)
- [Resource Management](#resource-management)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Cross-Platform**: Works on both Android and iOS using Kotlin Multiplatform
- **Type-Safe API**: Type-safe methods for common HTTP operations (GET, POST, PUT, PATCH, DELETE)
- **Flexible Authentication**:
  - Basic and bearer token authentication
  - Custom authentication with static and dynamic headers
  - Dynamic authentication based on HTTP method and URL path
  - Rule-based authentication for complex scenarios
  - Selective authentication with public endpoints
- **Robust Error Handling**: Structured exceptions with API response details
- **Configurable**: Logging, timeouts, retries, JSON serialization, and more
- **Resource Management**: Proper cleanup through Closeable interface

## Installation

Add the library to your shared module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.kmm.networkclient:networkclient:1.0.0")
}
```

## Basic Usage

Here's a simple example showing how to create a client and make HTTP requests:

```kotlin
// Create a network client
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        enableLogging = true
    )
)

// Make a GET request
suspend fun getUser(id: Int): User {
    return client.get("/users/$id")
}

// Make a POST request
suspend fun createUser(user: User): User {
    return client.post("/users", user)
}

// Don't forget to close the client when you're done
client.close()
```

## Authentication Options

The library provides a comprehensive set of authentication mechanisms to handle various API requirements:

### Basic Authentication

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        authConfig = NetworkClientConfig.AuthConfig.Basic(
            username = "apiuser",
            password = "apisecret"
        )
    )
)
```

### Bearer Token Authentication

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        authConfig = NetworkClientConfig.AuthConfig.Bearer(
            getToken = { "your-access-token" },
            refreshToken = "your-refresh-token" // optional
        )
    )
)
```

### Custom Authentication

Define your own authentication mechanism with static and dynamic headers:

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        authConfig = NetworkClientConfig.AuthConfig.Custom(
            headers = mapOf("X-Api-Key" to "your-api-key"),
            authenticator = { dynamicHeaders ->
                // Add dynamic headers for each request
                dynamicHeaders["X-Timestamp"] = System.currentTimeMillis().toString()
            }
        )
    )
)
```

### Dynamic Authentication

Apply different authentication methods based on HTTP method or request path:

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        authConfig = NetworkClientConfig.AuthConfig.Dynamic { method, path ->
            when (method) {
                HttpMethod.Get -> apiKeyAuth       // Simple API key for GET
                HttpMethod.Post -> bearerTokenAuth // Bearer token for POST
                else -> null                       // No auth for other methods
            }
        }
    )
)
```

### Rule-Based Authentication

For complex scenarios with multiple endpoints requiring different authentication:

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        authConfig = NetworkClientConfig.AuthConfig.RuleBased(
            rules = listOf(
                // Public endpoints use API key auth
                rule(
                    pathPattern = "/public/.*",
                    authConfig = apiKeyAuth
                ),
                
                // User endpoints use bearer token
                rule(
                    pathPattern = "/users/.*",
                    authConfig = bearerTokenAuth
                ),
                
                // Admin endpoints: different auth for different methods
                rule(
                    methods = setOf(HttpMethod.Get),
                    pathPattern = "/admin/.*",
                    authConfig = basicAuth
                ),
                rule(
                    methods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete),
                    pathPattern = "/admin/.*",
                    authConfig = signatureAuth
                )
            ),
            // Fallback if no rules match
            defaultConfig = apiKeyAuth
        )
    )
)
```

### No Authentication

Skip authentication for public endpoints:

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        authConfig = NetworkClientConfig.AuthConfig.RuleBased(
            rules = listOf(
                // Public endpoints - no authentication needed
                rule(
                    pathPattern = "/public/.*",
                    authConfig = NetworkClientConfig.AuthConfig.NoAuth
                ),
                
                // Health check endpoint - no authentication needed
                rule(
                    pathPattern = "/health",
                    authConfig = NetworkClientConfig.AuthConfig.NoAuth
                ),
                
                // Protected endpoints require authentication
                rule(
                    pathPattern = "/api/.*",
                    authConfig = bearerTokenAuth
                )
            )
        )
    )
)
```

Or conditionally with dynamic authentication:

```kotlin
val client = NetworkClient(
    NetworkClientConfig(
        authConfig = NetworkClientConfig.AuthConfig.Dynamic { method, path ->
            // Skip authentication for public endpoints and OPTIONS requests
            if (method == HttpMethod.Options || path.startsWith("/public/")) {
                NetworkClientConfig.AuthConfig.NoAuth
            } else {
                bearerTokenAuth
            }
        }
    )
)
```

## Error Handling

The library provides structured exceptions for different error types:

```kotlin
try {
    val user = client.get<User>("/users/$id")
} catch (e: NetworkException) {
    when (e) {
        is NetworkException.ClientError -> {
            // Handle 4xx errors
            println("Client error ${e.statusCode}: ${e.response}")
            
            // Handle specific client errors
            if (e.statusCode == 401) {
                // Handle unauthorized
            } else if (e.statusCode == 404) {
                // Handle not found
            }
        }
        is NetworkException.ServerError -> {
            // Handle 5xx errors
            println("Server error ${e.statusCode}: ${e.response}")
        }
        is NetworkException.UnknownError -> {
            // Handle other errors
            println("Unknown error: ${e.message}")
            e.cause?.printStackTrace()
        }
    }
}
```

## Resource Management

### Proper Cleanup

Always close the client when you're done with it:

```kotlin
// Create a client
val client = NetworkClient(config)

try {
    // Use the client...
} finally {
    // Clean up resources
    client.close()
}
```

### Using with Compose UI

In Compose UI, you can use `remember` and `DisposableEffect`:

```kotlin
@Composable
fun NetworkScreen() {
    val client = remember { NetworkClient(config) }
    
    DisposableEffect(Unit) {
        onDispose {
            client.close()
        }
    }
    
    // Use client in your composables...
}
```

### Service Pattern

Create a service class that handles resource cleanup:

```kotlin
class MyApiService : Closeable {
    private val client = NetworkClient(config)
    
    suspend fun getData(): Data {
        return client.get("/data")
    }
    
    override fun close() {
        client.close()
    }
}
```

## Project Structure

### Library Release Structure

When publishing this library:

```
shared/
  └── src/
      └── commonMain/
          └── kotlin/
              └── com/
                  └── kmm/
                      └── networkclient/
                          ├── NetworkClient.kt        // Core implementation
                          ├── NetworkClientConfig.kt  // Configuration options
                          ├── NetworkException.kt     // Exception handling
                          └── Closeable.kt            // Resource management
```

Important:
- **Remove sample code** from the shared module before publishing
- Move all samples to the composeApp module
- Include only essential components in the shared library

### Demo App Structure

The demo application should be structured as:

```
composeApp/
  └── src/
      └── commonMain/
          └── kotlin/
              └── com/
                  └── kmp/
                      └── network/
                          └── client/
                              ├── App.kt         // Main app entry
                              ├── DemoScreen.kt  // Demo UI
                              └── samples/       // Sample implementations
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

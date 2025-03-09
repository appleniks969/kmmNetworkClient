# KMM Network Client Library

A Kotlin Multiplatform Mobile (KMM) networking library that provides common functionality for making HTTP requests across Android and iOS platforms.

## Core Features

- Type-safe HTTP requests (GET, POST, PUT, DELETE, PATCH)
- Automatic serialization/deserialization with kotlinx.serialization
- Robust error handling
- Authentication support (Bearer and Basic)
- Configuration options for logging, timeouts, and retries
- Proper resource management with Closeable interface

## Project Structure Guidelines

### Library Release Structure

When publishing this library for external use, the structure should be:

```
shared/
  └── src/
      └── commonMain/
          └── kotlin/
              └── com/
                  └── kmm/
                      └── networkclient/
                          ├── NetworkClient.kt    // Core network client implementation
                          ├── NetworkClientConfig.kt  // Configuration options
                          ├── NetworkException.kt  // Exception handling
                          └── Closeable.kt  // Resource management interface
```

#### Important: 
- **REMOVE sample code** from the shared module before publishing
- Move any sample implementations to the composeApp module
- The shared library should only contain the essential components needed for the network client

### Demo App Structure

The demo implementation should be structured as:

```
composeApp/
  └── src/
      └── commonMain/
          └── kotlin/
              └── com/
                  └── kmp/
                      └── network/
                          └── client/
                              ├── App.kt  // Main app entry point
                              ├── DemoScreen.kt  // Demo UI for the network client
                              └── samples/  // Sample implementations should go here
                                  ├── UserModels.kt  // Data models for the demo
                                  └── UserService.kt  // Sample service implementation
```

## Usage

### Basic Usage

```kotlin
// Create a network client with configuration
val networkClient = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        enableLogging = true,
        retryConfig = NetworkClientConfig.RetryConfig(
            maxRetries = 2
        )
    )
)

// Make a GET request
val users: List<User> = networkClient.get("/users")

// Make a POST request
val user = User(name = "John", email = "john@example.com")
val response: CreateUserResponse = networkClient.post("/users", user)

// Remember to close the client to avoid resource leaks
networkClient.close()
```

### Recommended Resource Management

Use the client with proper resource management:

```kotlin
// In Compose UI
val networkClient = remember { NetworkClient(config) }

// Clean up resources when the composable is disposed
DisposableEffect(Unit) {
    onDispose {
        networkClient.close()
    }
}

// Or create a service that implements Closeable
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

## Publishing Guidelines

1. Ensure all sample code is moved out of the shared module
2. Update version number in `shared/build.gradle.kts`
3. Run tests to verify functionality
4. Run `./gradlew publish` to publish the library

## License

[MIT License](LICENSE)

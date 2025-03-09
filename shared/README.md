# KMM Network Client Library

A powerful and flexible Kotlin Multiplatform Mobile (KMM) network client library built on top of Ktor. This library provides a simple API for making HTTP requests while offering a rich set of features for both Android and iOS applications.

## Features

- 🌍 **Cross-Platform**: Works on both Android and iOS platforms using Kotlin Multiplatform
- 🔄 **Type-Safe API**: Type-safe methods for all common HTTP operations (GET, POST, PUT, PATCH, DELETE)
- 🔒 **Authentication**: Built-in support for token-based authentication with automatic token refresh
- 🔄 **Retry Handling**: Configurable retry behavior for failed requests
- 📦 **Request/Response Interceptors**: Customize requests and responses
- 📝 **Comprehensive Logging**: Configurable logging levels
- 📊 **Metrics Collection**: Track API performance and success rates
- 💾 **Caching**: Built-in caching for GET requests
- 📁 **File Operations**: Easy file uploads and downloads
- 📤 **Multipart Support**: Simple API for multipart/form-data uploads
- 📡 **Streaming**: Support for streaming responses using Kotlin Flow

## Installation

Add the library to your shared module:

```kotlin
dependencies {
    implementation("com.kmm.networkclient:networkclient:1.0.0")
}
```

## Basic Usage

Here's a simple example of how to use the library:

```kotlin
// Create a network client
val client = NetworkClient.builder()
    .baseUrl("https://api.example.com")
    .build()

// Make a GET request
suspend fun getUser(id: Int): User {
    return client.get("/users/$id")
}

// Make a POST request
suspend fun createUser(name: String, email: String): User {
    return client.post(
        path = "/users",
        body = mapOf("name" to name, "email" to email)
    )
}
```

## Advanced Configuration

The library is highly configurable through the builder pattern:

```kotlin
val client = NetworkClient.builder()
    .baseUrl("https://api.example.com")
    .addDefaultHeader("App-Version", "1.0.0")
    .connectTimeout(15.seconds)
    .readTimeout(30.seconds)
    .logLevel(LogLevel.HEADERS)
    .retry(maxRetries = 3)
    .authentication {
        initialToken = "current-token"
        refreshToken = "refresh-token"
        tokenRefresher = {
            // Implement token refresh logic here
            Pair("new-token", "new-refresh-token")
        }
    }
    .cache(enabled = true, maxSize = 100, ttl = 5 * 60 * 1000)
    .build()
```

## Error Handling

The library provides a structured exception hierarchy for easier error handling:

```kotlin
try {
    val user = client.get<User>("/users/$id")
    // Process user
} catch (e: NetworkException) {
    when (e) {
        is NetworkException.ClientError -> {
            if (e.isNotFound) {
                // Handle 404 error
            } else if (e.isUnauthorized) {
                // Handle 401 error
            } else {
                // Handle other client errors
            }
        }
        is NetworkException.ServerError -> {
            // Handle server errors (5xx)
        }
        is NetworkException.TimeoutError -> {
            // Handle timeout errors
        }
        else -> {
            // Handle other network errors
        }
    }
}
```

## Multipart Uploads

Uploading files is straightforward:

```kotlin
suspend fun uploadProfilePicture(userId: Int, imageBytes: ByteArray): User {
    return client.multipart(
        path = "/users/$userId/profile-picture",
        formData = {
            add("userId", userId.toString())
            add("image", "profile.jpg", imageBytes, "image/jpeg")
        }
    )
}
```

## Download Files

Downloading files is easy:

```kotlin
suspend fun downloadProfilePicture(userId: Int): ByteArray {
    return client.downloadFile("/users/$userId/profile-picture")
}
```

For large files, use the streaming API:

```kotlin
suspend fun downloadLargeFile(fileId: String, outputStream: OutputStream) {
    client.downloadFileAsFlow(
        path = "/files/$fileId"
    ).collect { chunk ->
        outputStream.write(chunk)
    }
}
```

## iOS Usage

On iOS, use the library through the Swift API:

```swift
import shared

func fetchUser(id: Int) async throws -> User {
    let service = UserService()
    return try await service.getUser(id: id)
}
```

## License

This library is licensed under the MIT License. 
# Kotlin Multiplatform Network Client Instructions

This repository contains a Kotlin Multiplatform (KMM) network client library that provides a shared implementation for both Android and iOS applications. The following instructions will help GitHub Copilot understand the project structure, coding standards, and design patterns.

## Project Overview

This is a Kotlin Multiplatform project that creates a shared network client for both Android and iOS applications. The primary goal is to maintain a single codebase for network operations while providing platform-specific optimizations.

### Key Components

- **NetworkClient**: Core class that handles HTTP requests/responses using Ktor
- **NetworkClientConfig**: Configuration class for customizing the client behavior
- **Platform-specific implementations**: Android and iOS-specific extensions and utilities

## Architecture

### Modules

1. **shared**: Contains the KMM network client implementation
   - **commonMain**: Common code shared between platforms
   - **androidMain**: Android-specific implementations
   - **iosMain**: iOS-specific implementations

2. **composeApp**: Android application using Jetpack Compose
   - Demonstrates using the shared network client in an Android context

3. **iosApp**: iOS application using SwiftUI
   - Demonstrates using the shared network client in iOS using Swift

## Code Style and Patterns

### Kotlin Conventions

- Use 4 spaces for indentation
- Follow Kotlin coding conventions with camelCase for variables and methods
- Classes should be named with PascalCase
- Constants should be in UPPER_SNAKE_CASE
- Prefer extension functions for platform-specific functionality
- Use suspending functions for asynchronous operations in Kotlin

### Shared Code Principles

- Keep platform-specific code to a minimum
- Use `expect`/`actual` declarations when necessary
- Implement platform-specific functionality through extension functions
- Use factory patterns for platform-specific implementations

### Network Client Design Patterns

- Builder pattern for NetworkClientConfig
- Strategy pattern for authentication implementations
- Factory pattern for platform-specific client implementations
- Support for custom interceptors/middleware (especially for Android)

## Authentication

The NetworkClient supports multiple authentication methods:

- Basic Auth
- Bearer Token
- Custom Auth
- Dynamic Auth (rule-based)
- No Auth

When implementing authentication, use the appropriate AuthConfig class from NetworkClientConfig.

## Error Handling

- NetworkException is used for common error cases
- Platform-specific error handling is implemented in respective platforms
- Error responses should be properly parsed and propagated

## Testing Approach

- Unit tests for common code
- Platform-specific tests for Android and iOS implementations
- Use mocks for network dependencies

## API Usage Examples

### Creating a Basic Network Client

```kotlin
// Common
val client = NetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        enableLogging = true
    )
)

// Android
val androidClient = createNetworkClient(
    NetworkClientConfig(
        baseUrl = "https://api.example.com",
        enableLogging = true
    )
)
```

### Making a GET Request

```kotlin
// Kotlin
suspend fun fetchData(path: String): String {
    return client.get(path)
}

// Swift
func fetchData(path: String) async throws -> String {
    return try await withCheckedThrowingContinuation { continuation in
        networkClient.get(path: path) { result, error in
            // Handle result and error
        }
    }
}
```

## Platform-Specific Concerns

### Android

- The Android implementation includes Chucker for network inspection
- Uses OkHttp as the engine for Ktor
- Special handling for Android Context

### iOS

- Uses Darwin engine for Ktor
- Implements Combine-friendly wrappers for Swift interoperability
- Exposed as a Swift framework

## Common Gotchas and Solutions

1. **iOS Framework Generation**
   - Always run `./copy_framework.sh` after making changes to the shared module
   - Ensure the framework is properly linked in Xcode

2. **Android Context**
   - Initialize AndroidContextProvider early in the application lifecycle

3. **Concurrency Models**
   - Android uses Kotlin coroutines
   - iOS uses Swift async/await and/or Combine

4. **Memory Management**
   - Always close the NetworkClient when finished to prevent memory leaks
   - Use structured concurrency on both platforms

## Feature Development Guidelines

When adding new features to the network client:

1. Start with the common implementation in commonMain
2. Add platform-specific code only when necessary
3. Update both Android and iOS sample apps to demonstrate the feature
4. Document the feature in comments and README
5. Add appropriate unit tests

## Preferred Libraries and Tools

- **HTTP Client**: Ktor
- **Serialization**: Kotlinx.serialization
- **Concurrency**: Kotlinx.coroutines
- **Logging**: Ktor built-in logging
- **Android Network Inspection**: Chucker
- **iOS UI**: SwiftUI
- **Android UI**: Jetpack Compose 
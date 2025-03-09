package com.kmm.networkclient

/**
 * Base class for network-related exceptions.
 */
sealed class NetworkException : Exception {
    constructor(message: String, cause: Throwable? = null) : super(message, cause)

    /**
     * Exception thrown when a client error occurs (HTTP 4xx).
     */
    class ClientError(
        message: String,
        val statusCode: Int,
        val response: String?
    ) : NetworkException(message)

    /**
     * Exception thrown when a server error occurs (HTTP 5xx).
     */
    class ServerError(
        message: String,
        val statusCode: Int,
        val response: String?
    ) : NetworkException(message)

    /**
     * Exception thrown when a timeout occurs.
     */
    class TimeoutError(
        message: String,
        cause: Throwable? = null
    ) : NetworkException(message, cause)

    /**
     * Exception thrown when an unknown error occurs.
     */
    class UnknownError(
        message: String,
        cause: Throwable? = null
    ) : NetworkException(message, cause)
} 
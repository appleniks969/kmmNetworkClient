package com.kmp.network.client.samples

import kotlinx.serialization.Serializable

/**
 * Sample data classes for API requests and responses
 */
@Serializable
data class User(val id: Int, val name: String, val email: String, val username: String = "")

@Serializable
data class CreateUserRequest(val name: String, val email: String)

@Serializable
data class CreateUserResponse(val id: Int, val createdAt: String) 
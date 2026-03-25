package com.inventorymanagementsystem.inventory_backend.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email(message = "A valid email is required")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
    @field:NotBlank(message = "Role is required")
    val role: String,
)

data class AuthResponse(
    val message: String,
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
)

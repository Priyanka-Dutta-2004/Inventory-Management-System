package com.inventorymanagementsystem.inventory_backend.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class UserAdminResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
    val status: UserStatus,
)

data class UserAdminSummaryResponse(
    val totalUsers: Int,
    val adminUsers: Int,
    val inactiveUsers: Int,
)

data class CreateUserRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:Email(message = "A valid email is required")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
    @field:NotBlank(message = "Role is required")
    val role: String,
    val status: UserStatus? = null,
)

data class UpdateUserRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    @field:Email(message = "A valid email is required")
    @field:NotBlank(message = "Email is required")
    val email: String,
    @field:NotBlank(message = "Role is required")
    val role: String,
    val status: UserStatus? = null,
)

data class UpdateUserStatusRequest(
    val status: UserStatus,
)

data class ResetUserPasswordRequest(
    @field:NotBlank(message = "Password is required")
    val password: String,
)

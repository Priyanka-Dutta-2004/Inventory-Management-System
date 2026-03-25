package com.inventorymanagementsystem.inventory_backend.auth

import com.inventorymanagementsystem.inventory_backend.user.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
) {

    fun login(request: LoginRequest): AuthResponse? {
        val user = userRepository.findByEmailIgnoreCase(request.email.trim()) ?: return null
        val requestedRole = request.role.trim().uppercase()
        val actualRole = runCatching { user.role.roleName.uppercase() }.getOrNull() ?: return null
        val storedPassword = runCatching { user.passwordHash }.getOrNull() ?: return null
        val userId = runCatching { user.userId }.getOrNull() ?: return null
        val userName = runCatching { user.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val userEmail = runCatching { user.email }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        if (actualRole != requestedRole) {
            return null
        }

        if (storedPassword != request.password) {
            return null
        }

        return AuthResponse(
            message = "Login successful",
            userId = userId,
            name = userName,
            email = userEmail,
            role = actualRole,
        )
    }
}

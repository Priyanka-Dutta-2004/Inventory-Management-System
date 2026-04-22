package com.inventorymanagementsystem.inventory_backend.user

import com.inventorymanagementsystem.inventory_backend.role.RoleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserAdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) {

    fun getSummary(): UserAdminSummaryResponse {
        val users = userRepository.findAll()
        return UserAdminSummaryResponse(
            totalUsers = users.size,
            adminUsers = users.count { it.role.roleName.equals("ADMIN", ignoreCase = true) },
            inactiveUsers = users.count { it.status == UserStatus.INACTIVE },
        )
    }

    fun listUsers(query: String?): List<UserAdminResponse> {
        val search = query.orEmpty().trim().lowercase()
        return userRepository.findAll()
            .asSequence()
            .filter { user ->
                if (search.isBlank()) {
                    true
                } else {
                    user.name.lowercase().contains(search) ||
                        user.email.lowercase().contains(search) ||
                        user.role.roleName.lowercase().contains(search)
                }
            }
            .sortedBy { it.userId ?: Long.MAX_VALUE }
            .map { it.toResponse() }
            .toList()
    }

    fun createUser(request: CreateUserRequest): UserAdminResponse {
        val name = request.name.trim()
        val email = request.email.trim()
        val password = request.password.trim()
        val roleName = request.role.trim()
        val status = request.status ?: UserStatus.ACTIVE

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")
        }

        val role = roleRepository.findByRoleNameIgnoreCase(roleName)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: $roleName")

        val saved = userRepository.save(
            UserEntity(
                name = name,
                email = email,
                passwordHash = password,
                role = role,
                status = status,
            )
        )

        return saved.toResponse()
    }

    fun updateUser(userId: Long, request: UpdateUserRequest): UserAdminResponse {
        val existing = findUser(userId)
        val name = request.name.trim()
        val email = request.email.trim()
        val roleName = request.role.trim()
        val status = request.status ?: existing.status

        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email already exists")
        }

        val role = roleRepository.findByRoleNameIgnoreCase(roleName)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: $roleName")

        val updated = userRepository.save(
            existing.copy(
                name = name,
                email = email,
                role = role,
                status = status,
            )
        )
        return updated.toResponse()
    }

    fun updateUserStatus(userId: Long, request: UpdateUserStatusRequest): UserAdminResponse {
        val existing = findUser(userId)
        val updated = userRepository.save(existing.copy(status = request.status))
        return updated.toResponse()
    }

    fun resetUserPassword(userId: Long, request: ResetUserPasswordRequest): UserAdminResponse {
        val existing = findUser(userId)
        val updated = userRepository.save(existing.copy(passwordHash = request.password.trim()))
        return updated.toResponse()
    }

    private fun findUser(userId: Long): UserEntity {
        return userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
    }

    private fun UserEntity.toResponse(): UserAdminResponse {
        return UserAdminResponse(
            userId = this.userId ?: 0L,
            name = this.name,
            email = this.email,
            role = this.role.roleName,
            status = this.status,
        )
    }
}

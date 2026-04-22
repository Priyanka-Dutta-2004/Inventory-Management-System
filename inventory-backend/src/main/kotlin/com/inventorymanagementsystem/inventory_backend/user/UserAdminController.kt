package com.inventorymanagementsystem.inventory_backend.user

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {

    @GetMapping
    fun listUsers(
        @RequestParam(required = false) query: String?,
    ): List<UserAdminResponse> {
        return userAdminService.listUsers(query)
    }

    @GetMapping("/summary")
    fun getSummary(): UserAdminSummaryResponse {
        return userAdminService.getSummary()
    }

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): UserAdminResponse {
        return userAdminService.createUser(request)
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserAdminResponse {
        return userAdminService.updateUser(userId, request)
    }

    @PatchMapping("/{userId}/status")
    fun updateUserStatus(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UpdateUserStatusRequest,
    ): UserAdminResponse {
        return userAdminService.updateUserStatus(userId, request)
    }

    @PatchMapping("/{userId}/password")
    fun resetUserPassword(
        @PathVariable userId: Long,
        @Valid @RequestBody request: ResetUserPasswordRequest,
    ): UserAdminResponse {
        return userAdminService.resetUserPassword(userId, request)
    }
}

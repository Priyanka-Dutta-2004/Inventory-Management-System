package com.inventorymanagementsystem.inventory_backend.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun existsByEmailIgnoreCaseAndUserIdNot(email: String, userId: Long): Boolean
    fun findByEmailIgnoreCase(email: String): UserEntity?
}

package com.inventorymanagementsystem.inventory_backend.role

import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<RoleEntity, Long> {
    fun findByRoleNameIgnoreCase(roleName: String): RoleEntity?
}

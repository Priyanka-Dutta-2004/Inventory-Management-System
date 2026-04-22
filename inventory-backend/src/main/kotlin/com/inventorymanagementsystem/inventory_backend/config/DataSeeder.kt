package com.inventorymanagementsystem.inventory_backend.config

import com.inventorymanagementsystem.inventory_backend.inventory.InventoryAssetEntity
import com.inventorymanagementsystem.inventory_backend.inventory.InventoryAssetRepository
import com.inventorymanagementsystem.inventory_backend.inventory.InventoryStatus
import com.inventorymanagementsystem.inventory_backend.role.RoleEntity
import com.inventorymanagementsystem.inventory_backend.role.RoleRepository
import com.inventorymanagementsystem.inventory_backend.user.UserEntity
import com.inventorymanagementsystem.inventory_backend.user.UserRepository
import com.inventorymanagementsystem.inventory_backend.user.UserStatus
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataSeeder {

    @Bean
    fun seedDefaultUsers(
        roleRepository: RoleRepository,
        userRepository: UserRepository,
        inventoryAssetRepository: InventoryAssetRepository,
    ): CommandLineRunner {
        return CommandLineRunner {
            val adminRole = ensureRole(roleRepository, "ADMIN")
            val managerRole = ensureRole(roleRepository, "MANAGER")
            val employeeRole = ensureRole(roleRepository, "EMPLOYEE")

            ensureUser(
                userRepository = userRepository,
                name = "System Admin",
                email = "admin@ims.com",
                password = "admin123",
                role = adminRole,
            )
            ensureUser(
                userRepository = userRepository,
                name = "Operations Manager",
                email = "manager@lnt.in",
                password = "1234@",
                role = managerRole,
            )
            ensureUser(
                userRepository = userRepository,
                name = "Inventory Employee",
                email = "user@ims.com",
                password = "user123",
                role = employeeRole,
            )

            ensureInventoryAsset(
                inventoryAssetRepository = inventoryAssetRepository,
                assetId = "LAP-2026-014",
                name = "Dell Latitude 5440",
                category = "Laptop",
                location = "Finance Floor",
                assignedTo = "Finance Team",
                status = InventoryStatus.IN_USE,
                condition = "Good",
                warrantyEndDate = "2027-03-15",
                lastAuditDate = "2026-03-12",
                notes = "Primary finance workstation with docking kit.",
            )
            ensureInventoryAsset(
                inventoryAssetRepository = inventoryAssetRepository,
                assetId = "NET-2025-022",
                name = "Cisco Access Switch",
                category = "Networking",
                location = "Server Room B",
                assignedTo = null,
                status = InventoryStatus.MAINTENANCE,
                condition = "Needs inspection",
                warrantyEndDate = "2026-12-20",
                lastAuditDate = "2026-03-10",
                notes = "Reactive ticket raised for port instability.",
            )
            ensureInventoryAsset(
                inventoryAssetRepository = inventoryAssetRepository,
                assetId = "MON-2025-067",
                name = "LG UltraFine 27-inch Monitor",
                category = "Monitor",
                location = "Storage Rack A2",
                assignedTo = null,
                status = InventoryStatus.AVAILABLE,
                condition = "Good",
                warrantyEndDate = "2026-04-07",
                lastAuditDate = "2026-03-19",
                notes = "Ready for reassignment after cleanup.",
            )
        }
    }

    private fun ensureRole(roleRepository: RoleRepository, roleName: String): RoleEntity {
        return roleRepository.findByRoleNameIgnoreCase(roleName)
            ?: roleRepository.save(RoleEntity(roleName = roleName))
    }

    private fun ensureUser(
        userRepository: UserRepository,
        name: String,
        email: String,
        password: String,
        role: RoleEntity,
    ) {
        val existingUser = userRepository.findByEmailIgnoreCase(email)
        if (existingUser != null) {
            val needsUpdate = runCatching {
                existingUser.name != name ||
                    existingUser.passwordHash != password ||
                    existingUser.status != UserStatus.ACTIVE ||
                    existingUser.role.roleName != role.roleName
            }.getOrDefault(true)

            if (needsUpdate) {
                userRepository.save(
                    existingUser.copy(
                        name = name,
                        passwordHash = password,
                        status = UserStatus.ACTIVE,
                        role = role,
                    )
                )
            }
            return
        }

        userRepository.save(
            UserEntity(
                name = name,
                email = email,
                passwordHash = password,
                status = UserStatus.ACTIVE,
                role = role,
            )
        )
    }

    private fun ensureInventoryAsset(
        inventoryAssetRepository: InventoryAssetRepository,
        assetId: String,
        name: String,
        category: String,
        location: String,
        assignedTo: String?,
        status: InventoryStatus,
        condition: String,
        warrantyEndDate: String,
        lastAuditDate: String,
        notes: String,
    ) {
        val existing = inventoryAssetRepository.findByAssetIdIgnoreCase(assetId)
        val normalizedAssignedTo = assignedTo?.takeUnless { it.isBlank() }
        val warrantyDate = java.time.LocalDate.parse(warrantyEndDate)
        val auditDate = java.time.LocalDate.parse(lastAuditDate)
        if (existing != null) {
            val needsUpdate = existing.name != name ||
                existing.category != category ||
                existing.location != location ||
                existing.assignedTo != normalizedAssignedTo ||
                existing.status != status ||
                existing.condition != condition ||
                existing.warrantyEndDate != warrantyDate ||
                existing.lastAuditDate != auditDate ||
                existing.notes != notes

            if (needsUpdate) {
                inventoryAssetRepository.save(
                    existing.copy(
                        name = name,
                        category = category,
                        location = location,
                        assignedTo = normalizedAssignedTo,
                        status = status,
                        condition = condition,
                        warrantyEndDate = warrantyDate,
                        lastAuditDate = auditDate,
                        notes = notes,
                    )
                )
            }
            return
        }

        inventoryAssetRepository.save(
            InventoryAssetEntity(
                assetId = assetId,
                name = name,
                category = category,
                location = location,
                assignedTo = normalizedAssignedTo,
                status = status,
                condition = condition,
                warrantyEndDate = warrantyDate,
                lastAuditDate = auditDate,
                notes = notes,
            )
        )
    }
}

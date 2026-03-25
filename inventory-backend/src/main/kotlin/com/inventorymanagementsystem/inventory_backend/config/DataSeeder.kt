package com.inventorymanagementsystem.inventory_backend.config

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
}

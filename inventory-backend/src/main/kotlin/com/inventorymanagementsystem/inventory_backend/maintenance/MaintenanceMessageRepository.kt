package com.inventorymanagementsystem.inventory_backend.maintenance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MaintenanceMessageRepository : JpaRepository<MaintenanceMessageEntity, Long> {
    fun findByTicketTicketIdOrderBySentAtAsc(ticketId: Long): List<MaintenanceMessageEntity>
}

package com.inventorymanagementsystem.inventory_backend.maintenance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MaintenanceTicketRepository : JpaRepository<MaintenanceTicketEntity, Long> {
    fun findByStatus(status: MaintenanceStatusEnum): List<MaintenanceTicketEntity>
    fun findByMaintenanceType(maintenanceType: MaintenanceTypeEnum): List<MaintenanceTicketEntity>
    fun findByStatusAndMaintenanceType(
        status: MaintenanceStatusEnum,
        maintenanceType: MaintenanceTypeEnum
    ): List<MaintenanceTicketEntity>
    fun findByReportedByUserIdOrderByCreatedAtDesc(userId: Long): List<MaintenanceTicketEntity>
    fun findByCreatedAtBetween(startDate: LocalDate, endDate: LocalDate): List<MaintenanceTicketEntity>
}

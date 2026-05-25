package com.inventorymanagementsystem.inventory_backend.maintenance

import java.time.LocalDate
import java.time.LocalDateTime

data class CreateMaintenanceTicketRequest(
    val assetId: String,
    val assetName: String,
    val description: String,
    val priority: String,
    val maintenanceType: MaintenanceTypeEnum = MaintenanceTypeEnum.MANUAL,
)

data class UpdateMaintenanceTicketStatusRequest(
    val status: MaintenanceStatusEnum,
)

data class AddMaintenanceMessageRequest(
    val content: String,
)

data class MaintenanceMessageDto(
    val messageId: Long,
    val senderName: String,
    val senderEmail: String,
    val content: String,
    val sentAt: LocalDateTime,
)

data class MaintenanceTicketDto(
    val ticketId: Long,
    val assetId: String,
    val assetName: String,
    val description: String,
    val priority: String,
    val status: MaintenanceStatusEnum,
    val maintenanceType: MaintenanceTypeEnum,
    val reportedByName: String,
    val reportedByEmail: String,
    val createdAt: LocalDate,
    val resolvedAt: LocalDate?,
    val messages: List<MaintenanceMessageDto> = emptyList(),
)

data class MaintenanceTicketListDto(
    val ticketId: Long,
    val assetName: String,
    val priority: String,
    val status: MaintenanceStatusEnum,
    val maintenanceType: MaintenanceTypeEnum,
    val reportedByName: String,
    val createdAt: LocalDate,
    val messageCount: Int,
)

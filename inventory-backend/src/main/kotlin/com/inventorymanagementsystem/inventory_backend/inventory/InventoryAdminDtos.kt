package com.inventorymanagementsystem.inventory_backend.inventory

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class InventoryAssetResponse(
    val id: Long,
    val assetId: String,
    val name: String,
    val category: String,
    val location: String,
    val assignedTo: String?,
    val status: InventoryStatus,
    val condition: String,
    val warrantyEndDate: LocalDate?,
    val lastAuditDate: LocalDate?,
    val notes: String,
)

data class InventorySummaryResponse(
    val availableAssets: Int,
    val allocatedAssets: Int,
    val retiredAssets: Int,
)

data class CreateInventoryAssetRequest(
    @field:NotBlank(message = "Asset ID is required")
    val assetId: String,
    @field:NotBlank(message = "Asset name is required")
    val name: String,
    @field:NotBlank(message = "Category is required")
    val category: String,
    @field:NotBlank(message = "Location is required")
    val location: String,
    val assignedTo: String? = null,
    val status: InventoryStatus? = null,
    @field:NotBlank(message = "Condition is required")
    val condition: String,
    val warrantyEndDate: LocalDate? = null,
    val lastAuditDate: LocalDate? = null,
    @field:NotBlank(message = "Notes are required")
    val notes: String,
)

data class UpdateInventoryAssetRequest(
    @field:NotBlank(message = "Asset ID is required")
    val assetId: String,
    @field:NotBlank(message = "Asset name is required")
    val name: String,
    @field:NotBlank(message = "Category is required")
    val category: String,
    @field:NotBlank(message = "Location is required")
    val location: String,
    val assignedTo: String? = null,
    val status: InventoryStatus? = null,
    @field:NotBlank(message = "Condition is required")
    val condition: String,
    val warrantyEndDate: LocalDate? = null,
    val lastAuditDate: LocalDate? = null,
    @field:NotBlank(message = "Notes are required")
    val notes: String,
)

data class UpdateInventoryStatusRequest(
    val status: InventoryStatus,
)

data class TransferInventoryAssetRequest(
    @field:NotBlank(message = "Location is required")
    val location: String,
    val assignedTo: String? = null,
)

data class BulkInventoryStatusRequest(
    val ids: List<Long>,
    val status: InventoryStatus,
)

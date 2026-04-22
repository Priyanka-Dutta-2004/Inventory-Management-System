package com.inventorymanagementsystem.inventory_backend.inventory

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
@RequestMapping("/api/admin/inventory")
class InventoryAdminController(
    private val inventoryAdminService: InventoryAdminService,
) {

    @GetMapping
    fun listAssets(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) status: InventoryStatus?,
    ): List<InventoryAssetResponse> {
        return inventoryAdminService.listAssets(query, status)
    }

    @GetMapping("/summary")
    fun getSummary(): InventorySummaryResponse {
        return inventoryAdminService.getSummary()
    }

    @PostMapping
    fun createAsset(@Valid @RequestBody request: CreateInventoryAssetRequest): InventoryAssetResponse {
        return inventoryAdminService.createAsset(request)
    }

    @PutMapping("/{assetId}")
    fun updateAsset(
        @PathVariable assetId: Long,
        @Valid @RequestBody request: UpdateInventoryAssetRequest,
    ): InventoryAssetResponse {
        return inventoryAdminService.updateAsset(assetId, request)
    }

    @PatchMapping("/{assetId}/status")
    fun updateStatus(
        @PathVariable assetId: Long,
        @Valid @RequestBody request: UpdateInventoryStatusRequest,
    ): InventoryAssetResponse {
        return inventoryAdminService.updateStatus(assetId, request)
    }

    @PatchMapping("/{assetId}/transfer")
    fun transferAsset(
        @PathVariable assetId: Long,
        @Valid @RequestBody request: TransferInventoryAssetRequest,
    ): InventoryAssetResponse {
        return inventoryAdminService.transferAsset(assetId, request)
    }

    @PatchMapping("/bulk-status")
    fun bulkUpdateStatus(
        @Valid @RequestBody request: BulkInventoryStatusRequest,
    ): List<InventoryAssetResponse> {
        return inventoryAdminService.bulkUpdateStatus(request)
    }
}

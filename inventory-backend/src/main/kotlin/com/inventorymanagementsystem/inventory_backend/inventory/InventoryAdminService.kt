package com.inventorymanagementsystem.inventory_backend.inventory

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InventoryAdminService(
    private val inventoryAssetRepository: InventoryAssetRepository,
) {

    fun listAssets(query: String?, status: InventoryStatus?): List<InventoryAssetResponse> {
        val search = query.orEmpty().trim().lowercase()
        return inventoryAssetRepository.findAll()
            .asSequence()
            .filter { asset ->
                status == null || asset.status == status
            }
            .filter { asset ->
                if (search.isBlank()) {
                    true
                } else {
                    asset.assetId.lowercase().contains(search) ||
                        asset.name.lowercase().contains(search) ||
                        asset.category.lowercase().contains(search) ||
                        asset.location.lowercase().contains(search) ||
                        asset.assignedTo.orEmpty().lowercase().contains(search) ||
                        asset.status.name.lowercase().contains(search)
                }
            }
            .sortedBy { it.assetId.lowercase() }
            .map { it.toResponse() }
            .toList()
    }

    fun getSummary(): InventorySummaryResponse {
        val assets = inventoryAssetRepository.findAll()
        return InventorySummaryResponse(
            availableAssets = assets.count { it.status == InventoryStatus.AVAILABLE },
            allocatedAssets = assets.count { it.status == InventoryStatus.IN_USE },
            retiredAssets = assets.count { it.status == InventoryStatus.RETIRED },
        )
    }

    fun createAsset(request: CreateInventoryAssetRequest): InventoryAssetResponse {
        val normalizedAssetId = request.assetId.trim()
        if (inventoryAssetRepository.existsByAssetIdIgnoreCase(normalizedAssetId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Asset ID already exists")
        }

        val saved = inventoryAssetRepository.save(
            InventoryAssetEntity(
                assetId = normalizedAssetId,
                name = request.name.trim(),
                category = request.category.trim(),
                location = request.location.trim(),
                assignedTo = request.assignedTo?.trim().takeUnless { it.isNullOrBlank() },
                status = request.status ?: InventoryStatus.AVAILABLE,
                condition = request.condition.trim(),
                warrantyEndDate = request.warrantyEndDate,
                lastAuditDate = request.lastAuditDate,
                notes = request.notes.trim(),
            )
        )
        return saved.toResponse()
    }

    fun updateAsset(assetId: Long, request: UpdateInventoryAssetRequest): InventoryAssetResponse {
        val existing = findAsset(assetId)
        val normalizedAssetId = request.assetId.trim()
        if (inventoryAssetRepository.existsByAssetIdIgnoreCaseAndIdNot(normalizedAssetId, assetId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Asset ID already exists")
        }

        val updated = inventoryAssetRepository.save(
            existing.copy(
                assetId = normalizedAssetId,
                name = request.name.trim(),
                category = request.category.trim(),
                location = request.location.trim(),
                assignedTo = request.assignedTo?.trim().takeUnless { it.isNullOrBlank() },
                status = request.status ?: existing.status,
                condition = request.condition.trim(),
                warrantyEndDate = request.warrantyEndDate,
                lastAuditDate = request.lastAuditDate,
                notes = request.notes.trim(),
            )
        )
        return updated.toResponse()
    }

    fun updateStatus(assetId: Long, request: UpdateInventoryStatusRequest): InventoryAssetResponse {
        val existing = findAsset(assetId)
        val updated = inventoryAssetRepository.save(
            existing.copy(
                status = request.status,
                assignedTo = if (request.status == InventoryStatus.AVAILABLE || request.status == InventoryStatus.RETIRED) null else existing.assignedTo,
            )
        )
        return updated.toResponse()
    }

    fun transferAsset(assetId: Long, request: TransferInventoryAssetRequest): InventoryAssetResponse {
        val existing = findAsset(assetId)
        val assignedTo = request.assignedTo?.trim().takeUnless { it.isNullOrBlank() }
        val nextStatus = if (assignedTo.isNullOrBlank()) InventoryStatus.AVAILABLE else InventoryStatus.IN_USE
        val updated = inventoryAssetRepository.save(
            existing.copy(
                location = request.location.trim(),
                assignedTo = assignedTo,
                status = nextStatus,
            )
        )
        return updated.toResponse()
    }

    fun bulkUpdateStatus(request: BulkInventoryStatusRequest): List<InventoryAssetResponse> {
        if (request.ids.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one asset")
        }

        val assets = inventoryAssetRepository.findAllById(request.ids)
        if (assets.size != request.ids.distinct().size) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "One or more assets were not found")
        }

        val updated = assets.map { asset ->
            asset.copy(
                status = request.status,
                assignedTo = if (request.status == InventoryStatus.AVAILABLE || request.status == InventoryStatus.RETIRED) null else asset.assignedTo,
            )
        }
        return inventoryAssetRepository.saveAll(updated).map { it.toResponse() }
    }

    private fun findAsset(assetId: Long): InventoryAssetEntity {
        return inventoryAssetRepository.findById(assetId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found")
        }
    }

    private fun InventoryAssetEntity.toResponse(): InventoryAssetResponse {
        return InventoryAssetResponse(
            id = this.id ?: 0L,
            assetId = this.assetId,
            name = this.name,
            category = this.category,
            location = this.location,
            assignedTo = this.assignedTo,
            status = this.status,
            condition = this.condition,
            warrantyEndDate = this.warrantyEndDate,
            lastAuditDate = this.lastAuditDate,
            notes = this.notes,
        )
    }
}

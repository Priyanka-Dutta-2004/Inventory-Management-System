package com.inventorymanagementsystem.inventory_backend.inventory

import org.springframework.data.jpa.repository.JpaRepository

interface InventoryAssetRepository : JpaRepository<InventoryAssetEntity, Long> {
    fun existsByAssetIdIgnoreCase(assetId: String): Boolean
    fun existsByAssetIdIgnoreCaseAndIdNot(assetId: String, id: Long): Boolean
    fun findByAssetIdIgnoreCase(assetId: String): InventoryAssetEntity?
}

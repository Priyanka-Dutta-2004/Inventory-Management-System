package com.inventorymanagementsystem.inventory_backend.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "inventory_assets")
data class InventoryAssetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_pk")
    val id: Long? = null,
    @Column(name = "asset_id", nullable = false, unique = true)
    val assetId: String,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val category: String,
    @Column(nullable = false)
    val location: String,
    @Column(name = "assigned_to")
    val assignedTo: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: InventoryStatus = InventoryStatus.AVAILABLE,
    @Column(nullable = false)
    val condition: String,
    @Column(name = "warranty_end_date")
    val warrantyEndDate: LocalDate? = null,
    @Column(name = "last_audit_date")
    val lastAuditDate: LocalDate? = null,
    @Column(nullable = false, length = 1000)
    val notes: String,
)

enum class InventoryStatus {
    AVAILABLE,
    IN_USE,
    MAINTENANCE,
    RETIRED,
}

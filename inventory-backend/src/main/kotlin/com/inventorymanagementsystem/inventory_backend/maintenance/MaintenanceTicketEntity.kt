package com.inventorymanagementsystem.inventory_backend.maintenance

import com.inventorymanagementsystem.inventory_backend.user.UserEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate

enum class MaintenanceTypeEnum {
    MANUAL, SCHEDULED
}

enum class MaintenanceStatusEnum {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}

@Entity
@Table(name = "maintenance_tickets")
data class MaintenanceTicketEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    val ticketId: Long? = null,

    @Column(nullable = false)
    val assetId: String,

    @Column(nullable = false)
    val assetName: String,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val priority: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MaintenanceStatusEnum = MaintenanceStatusEnum.OPEN,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val maintenanceType: MaintenanceTypeEnum = MaintenanceTypeEnum.MANUAL,

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reported_by_id", nullable = false)
    val reportedBy: UserEntity,

    @Column(nullable = false)
    val createdAt: LocalDate = LocalDate.now(),

    @Column(nullable = true)
    val resolvedAt: LocalDate? = null,

    @OneToMany(
        mappedBy = "ticket",
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER
    )
    val messages: List<MaintenanceMessageEntity> = emptyList(),
)

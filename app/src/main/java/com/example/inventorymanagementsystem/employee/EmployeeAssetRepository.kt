package com.example.inventorymanagementsystem.employee

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

enum class EmployeeAssetState {
    HEALTHY,
    ATTENTION,
}

data class EmployeeAsset(
    val name: String,
    val assetId: String,
    val category: String,
    val assignedDate: LocalDate,
    val coverageType: String,
    val coverageEndDate: LocalDate,
    val location: String,
    val statusLabel: String,
    val state: EmployeeAssetState,
    val tags: List<String>,
)

data class EmployeeActivity(
    val title: String,
    val description: String,
)

data class EmployeeAssetSummary(
    val totalAssets: Int,
    val healthyAssets: Int,
    val attentionAssets: Int,
    val requests: Int,
    val alerts: Int,
)

object EmployeeAssetRepository {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun getAssets(session: EmployeeSession?): List<EmployeeAsset> {
        val location = session?.name?.takeIf { it.isNotBlank() }?.let { "$it workspace" } ?: "Desk B12"
        return listOf(
            EmployeeAsset(
                name = "Dell Latitude 5440",
                assetId = "LAP-2026-014",
                category = "Laptop",
                assignedDate = LocalDate.of(2026, 1, 5),
                coverageType = "Warranty",
                coverageEndDate = LocalDate.of(2027, 1, 12),
                location = location,
                statusLabel = "In use",
                state = EmployeeAssetState.HEALTHY,
                tags = listOf("Primary device", "Warranty active"),
            ),
            EmployeeAsset(
                name = "LG UltraFine 27-inch Monitor",
                assetId = "MON-2025-067",
                category = "Monitor",
                assignedDate = LocalDate.of(2025, 11, 19),
                coverageType = "AMC",
                coverageEndDate = LocalDate.of(2026, 4, 28),
                location = "Finance Bay B12",
                statusLabel = "Workstation",
                state = EmployeeAssetState.ATTENTION,
                tags = listOf("Desk setup", "Expiry soon"),
            ),
            EmployeeAsset(
                name = "Samsung Galaxy S24",
                assetId = "MOB-2026-009",
                category = "Mobile",
                assignedDate = LocalDate.of(2026, 2, 10),
                coverageType = "Insurance",
                coverageEndDate = LocalDate.of(2026, 12, 18),
                location = "Corporate SIM enabled",
                statusLabel = "Mobile",
                state = EmployeeAssetState.HEALTHY,
                tags = listOf("Travel ready", "Good condition"),
            ),
        )
    }

    fun buildSummary(assets: List<EmployeeAsset>): EmployeeAssetSummary {
        val healthyAssets = assets.count { it.state == EmployeeAssetState.HEALTHY }
        val attentionAssets = assets.count { it.state == EmployeeAssetState.ATTENTION }
        return EmployeeAssetSummary(
            totalAssets = assets.size,
            healthyAssets = healthyAssets,
            attentionAssets = attentionAssets,
            requests = 2,
            alerts = attentionAssets,
        )
    }

    fun buildRecentActivity(assets: List<EmployeeAsset>): List<EmployeeActivity> {
        val today = LocalDate.now()
        val activity = mutableListOf<EmployeeActivity>()

        assets
            .sortedBy { it.coverageEndDate }
            .take(2)
            .forEach { asset ->
                val daysLeft = max(0, ChronoUnit.DAYS.between(today, asset.coverageEndDate).toInt())
                val description = if (asset.state == EmployeeAssetState.ATTENTION) {
                    "${asset.coverageType} expires in $daysLeft days for ${asset.assetId}"
                } else {
                    "${asset.coverageType} active until ${asset.coverageEndDate.format(formatter)}"
                }
                activity += EmployeeActivity(asset.name, description)
            }

        activity += EmployeeActivity(
            title = "Pending request review",
            description = "Docking station request is awaiting manager approval",
        )

        return activity
    }

    fun buildHealthSummary(assets: List<EmployeeAsset>): String {
        val summary = buildSummary(assets)
        return "${summary.healthyAssets} healthy, ${summary.attentionAssets} need attention"
    }

    fun formatMeta(asset: EmployeeAsset): String {
        return "Asset ID ${asset.assetId} | Issued ${asset.assignedDate.format(formatter)} | ${asset.coverageType} until ${asset.coverageEndDate.format(formatter)}"
    }
}

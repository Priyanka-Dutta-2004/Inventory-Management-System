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
        return emptyList()
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

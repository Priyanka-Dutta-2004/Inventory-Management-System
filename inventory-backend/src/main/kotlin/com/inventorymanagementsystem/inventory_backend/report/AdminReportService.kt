package com.inventorymanagementsystem.inventory_backend.report

import com.inventorymanagementsystem.inventory_backend.inventory.InventoryAssetEntity
import com.inventorymanagementsystem.inventory_backend.inventory.InventoryAssetRepository
import com.inventorymanagementsystem.inventory_backend.inventory.InventoryStatus
import com.inventorymanagementsystem.inventory_backend.user.UserRepository
import com.inventorymanagementsystem.inventory_backend.user.UserStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AdminReportService(
    private val inventoryAssetRepository: InventoryAssetRepository,
    private val userRepository: UserRepository,
) {
    fun getSummary(): AdminReportSummaryResponse {
        val today = LocalDate.now()
        val assets = inventoryAssetRepository.findAll()
        return AdminReportSummaryResponse(
            totalAssets = assets.size,
            overdueAudits = assets.count { asset ->
                val lastAuditDate = asset.lastAuditDate
                lastAuditDate == null || lastAuditDate.isBefore(today.minusDays(90))
            },
            maintenanceAssets = assets.count { it.status == InventoryStatus.MAINTENANCE },
            expiringAssets = assets.count { asset ->
                val warrantyEndDate = asset.warrantyEndDate
                warrantyEndDate != null && !warrantyEndDate.isBefore(today) && !warrantyEndDate.isAfter(today.plusDays(30))
            },
        )
    }

    fun previewReport(
        reportType: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        department: String?,
    ): AdminReportPreviewResponse {
        val normalizedType = normalizeReportType(reportType)
        val normalizedDepartment = department?.trim()?.takeIf { it.isNotBlank() }
        val generatedOn = LocalDate.now().toString()
        val rows = when (normalizedType) {
            "WARRANTY_EXPIRY" -> warrantyRows(fromDate, toDate, normalizedDepartment)
            "AUDIT_LOG_EXPORT" -> auditRows(normalizedDepartment)
            else -> inventoryRows(normalizedDepartment)
        }
        val title = when (normalizedType) {
            "WARRANTY_EXPIRY" -> "Warranty expiry report"
            "AUDIT_LOG_EXPORT" -> "Audit compliance report"
            else -> "Inventory utilization report"
        }
        val exportText = buildExportText(
            title = title,
            generatedOn = generatedOn,
            fromDate = fromDate?.toString(),
            toDate = toDate?.toString(),
            department = normalizedDepartment,
            rows = rows,
        )
        return AdminReportPreviewResponse(
            reportType = normalizedType,
            title = title,
            generatedOn = generatedOn,
            appliedDepartmentFilter = normalizedDepartment,
            appliedFromDate = fromDate?.toString(),
            appliedToDate = toDate?.toString(),
            rows = rows,
            exportText = exportText,
        )
    }

    private fun inventoryRows(department: String?): List<String> {
        return filterAssetsByDepartment(department)
            .sortedBy { it.assetId }
            .map {
                "${it.assetId} | ${it.name} | ${it.status.name} | ${it.assignedTo ?: it.location}"
            }
            .ifEmpty { listOf("No inventory records matched the selected filters.") }
    }

    private fun warrantyRows(fromDate: LocalDate?, toDate: LocalDate?, department: String?): List<String> {
        val today = LocalDate.now()
        val start = fromDate ?: today
        val end = toDate ?: today.plusDays(30)
        return filterAssetsByDepartment(department)
            .filter { asset ->
                val warrantyEndDate = asset.warrantyEndDate
                warrantyEndDate != null && !warrantyEndDate.isBefore(start) && !warrantyEndDate.isAfter(end)
            }
            .sortedBy { it.warrantyEndDate }
            .map {
                "${it.assetId} | ${it.name} | Warranty ends ${it.warrantyEndDate} | ${it.assignedTo ?: it.location}"
            }
            .ifEmpty { listOf("No warranty expiry records matched the selected range.") }
    }

    private fun auditRows(department: String?): List<String> {
        val today = LocalDate.now()
        val assetRows = filterAssetsByDepartment(department)
            .filter { asset ->
                val lastAuditDate = asset.lastAuditDate
                lastAuditDate == null || lastAuditDate.isBefore(today.minusDays(90)) || asset.status == InventoryStatus.MAINTENANCE
            }
            .sortedBy { it.assetId }
            .map {
                val auditLabel = it.lastAuditDate?.toString() ?: "Never audited"
                "${it.assetId} | ${it.name} | Last audit: $auditLabel | Status: ${it.status.name}"
            }

        val userRows = userRepository.findAll()
            .asSequence()
            .filter { user ->
                department.isNullOrBlank() ||
                    user.name.contains(department, ignoreCase = true) ||
                    user.email.contains(department, ignoreCase = true)
            }
            .filter { it.status == UserStatus.INACTIVE }
            .sortedBy { it.userId ?: Long.MAX_VALUE }
            .map {
                "USER-${it.userId} | ${it.name} | ${it.email} | Status: ${it.status.name}"
            }
            .toList()

        return (assetRows + userRows).ifEmpty { listOf("No audit compliance exceptions matched the selected filters.") }
    }

    private fun filterAssetsByDepartment(department: String?): List<InventoryAssetEntity> {
        if (department.isNullOrBlank()) {
            return inventoryAssetRepository.findAll()
        }
        return inventoryAssetRepository.findAll().filter { asset ->
            asset.assignedTo.orEmpty().contains(department, ignoreCase = true) ||
                asset.location.contains(department, ignoreCase = true) ||
                asset.category.contains(department, ignoreCase = true)
        }
    }

    private fun normalizeReportType(reportType: String?): String {
        return when (reportType.orEmpty().trim().replace(' ', '_').uppercase()) {
            "WARRANTY_EXPIRY" -> "WARRANTY_EXPIRY"
            "AUDIT_LOG_EXPORT", "AUDIT_COMPLIANCE" -> "AUDIT_LOG_EXPORT"
            else -> "INVENTORY_SUMMARY"
        }
    }

    private fun buildExportText(
        title: String,
        generatedOn: String,
        fromDate: String?,
        toDate: String?,
        department: String?,
        rows: List<String>,
    ): String {
        val header = buildList {
            add(title)
            add("Generated on: $generatedOn")
            if (!fromDate.isNullOrBlank()) add("From: $fromDate")
            if (!toDate.isNullOrBlank()) add("To: $toDate")
            if (!department.isNullOrBlank()) add("Department filter: $department")
        }
        return (header + "" + rows).joinToString("\n")
    }
}

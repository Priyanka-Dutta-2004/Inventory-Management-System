package com.inventorymanagementsystem.inventory_backend.report

data class AdminReportSummaryResponse(
    val totalAssets: Int,
    val overdueAudits: Int,
    val maintenanceAssets: Int,
    val expiringAssets: Int,
)

data class AdminReportPreviewResponse(
    val reportType: String,
    val title: String,
    val generatedOn: String,
    val appliedDepartmentFilter: String?,
    val appliedFromDate: String?,
    val appliedToDate: String?,
    val rows: List<String>,
    val exportText: String,
)

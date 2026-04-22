package com.example.inventorymanagementsystem.admin

import com.example.inventorymanagementsystem.network.BackendApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class AdminReportSummary(
    val totalAssets: Int,
    val overdueAudits: Int,
    val maintenanceAssets: Int,
    val expiringAssets: Int,
)

data class AdminReportPreview(
    val reportType: String,
    val title: String,
    val generatedOn: String,
    val appliedDepartmentFilter: String?,
    val appliedFromDate: String?,
    val appliedToDate: String?,
    val rows: List<String>,
    val exportText: String,
)

object AdminReportApiClient {
    fun getSummary(
        onSuccess: (AdminReportSummary) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/reports/summary")
                    .get()
                    .build()
            },
            onSuccess = { body ->
                val json = JSONObject(body)
                onSuccess(
                    AdminReportSummary(
                        totalAssets = json.optInt("totalAssets"),
                        overdueAudits = json.optInt("overdueAudits"),
                        maintenanceAssets = json.optInt("maintenanceAssets"),
                        expiringAssets = json.optInt("expiringAssets"),
                    )
                )
            },
            onError = onError,
        )
    }

    fun previewReport(
        reportType: String,
        fromDate: String?,
        toDate: String?,
        department: String?,
        onSuccess: (AdminReportPreview) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                val httpUrl = "${baseUrl}api/admin/reports/preview"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("reportType", reportType)
                    .apply {
                        if (!fromDate.isNullOrBlank()) addQueryParameter("fromDate", fromDate)
                        if (!toDate.isNullOrBlank()) addQueryParameter("toDate", toDate)
                        if (!department.isNullOrBlank()) addQueryParameter("department", department)
                    }
                    .build()
                Request.Builder().url(httpUrl).get().build()
            },
            onSuccess = { body ->
                val json = JSONObject(body)
                val rowsJson = json.optJSONArray("rows") ?: JSONArray()
                val rows = mutableListOf<String>()
                for (index in 0 until rowsJson.length()) {
                    rows += rowsJson.optString(index)
                }
                onSuccess(
                    AdminReportPreview(
                        reportType = json.optString("reportType"),
                        title = json.optString("title"),
                        generatedOn = json.optString("generatedOn"),
                        appliedDepartmentFilter = json.optString("appliedDepartmentFilter").takeIf { it.isNotBlank() },
                        appliedFromDate = json.optString("appliedFromDate").takeIf { it.isNotBlank() },
                        appliedToDate = json.optString("appliedToDate").takeIf { it.isNotBlank() },
                        rows = rows,
                        exportText = json.optString("exportText"),
                    )
                )
            },
            onError = onError,
        )
    }

}

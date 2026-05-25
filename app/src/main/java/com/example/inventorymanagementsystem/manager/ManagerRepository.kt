package com.example.inventorymanagementsystem.manager

import android.content.Context
import com.example.inventorymanagementsystem.employee.EmployeeRequestRepository
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class ManagerRequestDecision {
    PENDING,
    APPROVED,
    REJECTED,
}

data class ManagerRequestRecord(
    val id: Long,
    val employeeName: String,
    val employeeEmail: String,
    val category: String,
    val assetName: String,
    val justification: String,
    val priority: String,
    val neededBy: LocalDate,
    val createdAt: LocalDate,
    val decision: ManagerRequestDecision,
    val managerComment: String?,
    val decidedAt: LocalDateTime?,
)

data class ManagerTeamAsset(
    val assetId: String,
    val name: String,
    val category: String,
    val owner: String,
    val department: String,
    val location: String,
    val status: String,
    val coverageType: String,
    val coverageEndDate: LocalDate,
)

data class ManagerReportSummary(
    val totalAssets: Int,
    val pendingRequests: Int,
    val expiringAssets: Int,
    val maintenanceAssets: Int,
)

data class ManagerReportPreview(
    val title: String,
    val generatedOn: String,
    val appliedDepartmentFilter: String?,
    val appliedFromDate: String?,
    val appliedToDate: String?,
    val rows: List<String>,
    val exportText: String,
)

object ManagerRepository {
    private const val preferencesName = "manager_request_reviews"
    private const val keyDecisions = "request_decisions"
    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH)
    private val displayDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val isoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getRequests(context: Context): List<ManagerRequestRecord> {
        val decisionMap = getDecisionMap(context)
        val seeded = seededRequests()
        val submitted = EmployeeRequestRepository.getSubmittedRequests(context).map { request ->
            val savedDecision = decisionMap[request.id]
            ManagerRequestRecord(
                id = request.id,
                employeeName = request.employeeName,
                employeeEmail = request.employeeEmail,
                category = request.category,
                assetName = request.assetName,
                justification = request.justification,
                priority = request.priority,
                neededBy = request.neededBy,
                createdAt = request.createdAt,
                decision = savedDecision?.decision ?: request.status.toDecision(),
                managerComment = savedDecision?.comment,
                decidedAt = savedDecision?.decidedAt,
            )
        }

        return (submitted + seeded.map { template ->
            val savedDecision = decisionMap[template.id]
            template.copy(
                decision = savedDecision?.decision ?: template.decision,
                managerComment = savedDecision?.comment ?: template.managerComment,
                decidedAt = savedDecision?.decidedAt ?: template.decidedAt,
            )
        }).sortedWith(
            compareByDescending<ManagerRequestRecord> { it.decision == ManagerRequestDecision.PENDING }
                .thenByDescending { it.createdAt }
                .thenByDescending { it.id }
        )
    }

    fun getRequest(context: Context, requestId: Long): ManagerRequestRecord? {
        return getRequests(context).firstOrNull { it.id == requestId }
    }

    fun saveDecision(
        context: Context,
        requestId: Long,
        decision: ManagerRequestDecision,
        comment: String?,
    ): ManagerRequestRecord? {
        val current = getRequest(context, requestId) ?: return null
        val savedDecision = SavedDecision(
            decision = decision,
            comment = comment?.trim()?.takeIf { it.isNotBlank() },
            decidedAt = LocalDateTime.now(),
        )
        persistDecision(context, requestId, savedDecision)
        // When manager approves, escalate to admin for final acceptance; otherwise set the final label
        val targetStatus = when (decision) {
            ManagerRequestDecision.APPROVED -> "Pending admin review"
            else -> decision.toStatusLabel()
        }
        EmployeeRequestRepository.updateRequestStatus(context, requestId, targetStatus)
        return current.copy(
            decision = decision,
            managerComment = savedDecision.comment,
            decidedAt = savedDecision.decidedAt,
        )
    }

    fun getTeamAssets(): List<ManagerTeamAsset> {
        return emptyList()
    }

    fun buildSummary(context: Context): ManagerReportSummary {
        val assets = getTeamAssets()
        val requests = getRequests(context)
        return ManagerReportSummary(
            totalAssets = assets.size,
            pendingRequests = requests.count { it.decision == ManagerRequestDecision.PENDING },
            expiringAssets = assets.count { it.coverageEndDate <= LocalDate.now().plusDays(45) },
            maintenanceAssets = assets.count { it.status.equals("Maintenance", ignoreCase = true) },
        )
    }

    fun previewReport(
        context: Context,
        reportType: String,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        department: String?,
    ): ManagerReportPreview {
        val normalizedType = normalizeReportType(reportType)
        val generatedOn = LocalDate.now().format(displayDateFormatter)
        val assets = getTeamAssets().filter { asset ->
            department.isNullOrBlank() || asset.department.contains(department, ignoreCase = true)
        }
        val requests = getRequests(context).filter { request ->
            val matchesDepartment = department.isNullOrBlank() ||
                request.category.contains(department, ignoreCase = true) ||
                request.employeeName.contains(department, ignoreCase = true)
            val matchesFrom = fromDate == null || !request.createdAt.isBefore(fromDate)
            val matchesTo = toDate == null || !request.createdAt.isAfter(toDate)
            matchesDepartment && matchesFrom && matchesTo
        }

        val title = when (normalizedType) {
            "PENDING_REQUESTS" -> "Pending approvals"
            "WARRANTY_RISK" -> "Warranty and coverage watchlist"
            "MAINTENANCE" -> "Assets under maintenance"
            else -> "Team asset utilization"
        }

        val rows = when (normalizedType) {
            "PENDING_REQUESTS" -> {
                if (requests.isEmpty()) {
                    listOf("No requests match the selected filters.")
                } else {
                    requests.map { request ->
                        "${request.assetName} | ${request.employeeName} | ${request.priority} | ${request.decision.toStatusLabel()} | Needed ${request.neededBy.format(displayDateFormatter)}"
                    }
                }
            }
            "WARRANTY_RISK" -> {
                val expiringAssets = assets.filter { it.coverageEndDate <= LocalDate.now().plusDays(45) }
                if (expiringAssets.isEmpty()) {
                    listOf("No assets are nearing expiry in the next 45 days.")
                } else {
                    expiringAssets.map { asset ->
                        "${asset.name} | ${asset.assetId} | ${asset.department} | ${asset.coverageType} ends ${asset.coverageEndDate.format(displayDateFormatter)}"
                    }
                }
            }
            "MAINTENANCE" -> {
                val maintenanceAssets = assets.filter { it.status.equals("Maintenance", ignoreCase = true) }
                if (maintenanceAssets.isEmpty()) {
                    listOf("No assets are currently under maintenance.")
                } else {
                    maintenanceAssets.map { asset ->
                        "${asset.name} | ${asset.assetId} | ${asset.location} | ${asset.owner}"
                    }
                }
            }
            else -> {
                if (assets.isEmpty()) {
                    listOf("No assets match the selected filters.")
                } else {
                    assets.map { asset ->
                        "${asset.name} | ${asset.assetId} | ${asset.department} | ${asset.status}"
                    }
                }
            }
        }

        val exportLines = buildList {
            add(title)
            add("Generated on $generatedOn")
            if (!department.isNullOrBlank()) add("Department filter: $department")
            if (fromDate != null) add("From: ${fromDate.format(displayDateFormatter)}")
            if (toDate != null) add("To: ${toDate.format(displayDateFormatter)}")
            add("")
            addAll(rows)
        }

        return ManagerReportPreview(
            title = title,
            generatedOn = generatedOn,
            appliedDepartmentFilter = department?.takeIf { it.isNotBlank() },
            appliedFromDate = fromDate?.format(displayDateFormatter),
            appliedToDate = toDate?.format(displayDateFormatter),
            rows = rows,
            exportText = exportLines.joinToString("\n"),
        )
    }

    fun formatRequestDate(value: LocalDate): String = value.format(displayDateFormatter)

    fun formatDecisionDateTime(value: LocalDateTime): String = value.format(displayDateTimeFormatter)

    private fun seededRequests(): List<ManagerRequestRecord> {
        return emptyList()
    }

    private fun normalizeReportType(value: String): String {
        return when (value.trim().replace(' ', '_').uppercase(Locale.ENGLISH)) {
            "PENDING_REQUESTS", "APPROVALS" -> "PENDING_REQUESTS"
            "WARRANTY_RISK", "EXPIRY", "WARRANTY_EXPIRY" -> "WARRANTY_RISK"
            "MAINTENANCE", "SERVICE" -> "MAINTENANCE"
            else -> "TEAM_ASSET_UTILIZATION"
        }
    }

    private fun getDecisionMap(context: Context): Map<Long, SavedDecision> {
        val raw = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyDecisions, null)
            ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associate { key ->
                val item = json.getJSONObject(key)
                key.toLong() to SavedDecision(
                    decision = item.optString("decision").toDecision(),
                    comment = item.optString("comment").takeIf { it.isNotBlank() },
                    decidedAt = item.optString("decidedAt").takeIf { it.isNotBlank() }
                        ?.let { LocalDateTime.parse(it, isoDateTimeFormatter) },
                )
            }
        }.getOrDefault(emptyMap())
    }

    private fun persistDecision(
        context: Context,
        requestId: Long,
        decision: SavedDecision,
    ) {
        val currentJson = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyDecisions, null)
            ?.let(::JSONObject)
            ?: JSONObject()
        currentJson.put(
            requestId.toString(),
            JSONObject()
                .put("decision", decision.decision.name)
                .put("comment", decision.comment)
                .put("decidedAt", decision.decidedAt?.format(isoDateTimeFormatter))
        )
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(keyDecisions, currentJson.toString())
            .apply()
    }

    private data class SavedDecision(
        val decision: ManagerRequestDecision,
        val comment: String?,
        val decidedAt: LocalDateTime?,
    )

    private fun String.toDecision(): ManagerRequestDecision {
        return when (trim().uppercase(Locale.ENGLISH)) {
            "APPROVED" -> ManagerRequestDecision.APPROVED
            "FULFILLED" -> ManagerRequestDecision.APPROVED
            "ASSIGNED" -> ManagerRequestDecision.APPROVED
            "REJECTED" -> ManagerRequestDecision.REJECTED
            "PENDING MANAGER REVIEW", "PENDING" -> ManagerRequestDecision.PENDING
            else -> ManagerRequestDecision.PENDING
        }
    }
}

fun ManagerRequestDecision.toStatusLabel(): String {
    return when (this) {
        ManagerRequestDecision.PENDING -> "Pending manager review"
        ManagerRequestDecision.APPROVED -> "Approved"
        ManagerRequestDecision.REJECTED -> "Rejected"
    }
}

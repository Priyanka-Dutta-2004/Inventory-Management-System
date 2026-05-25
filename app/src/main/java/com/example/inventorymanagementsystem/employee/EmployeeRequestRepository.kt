package com.example.inventorymanagementsystem.employee

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class EmployeeAssetRequestRecord(
    val id: Long,
    val employeeName: String,
    val employeeEmail: String,
    val category: String,
    val assetName: String,
    val justification: String,
    val priority: String,
    val neededBy: LocalDate,
    val status: String,
    val createdAt: LocalDate,
    val fulfilledAssetId: String? = null,
    val fulfilledAt: LocalDate? = null,
)

data class EmployeeAssetRequestDraft(
    val category: String? = null,
    val assetName: String = "",
    val justification: String = "",
    val priority: String = "",
    val neededBy: LocalDate? = null,
)

object EmployeeRequestRepository {
    private const val preferencesName = "employee_requests"
    private const val keyRequests = "submitted_requests"
    private const val keyDraft = "request_draft"
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun saveDraft(context: Context, draft: EmployeeAssetRequestDraft) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(keyDraft, draftToJson(draft).toString())
            .apply()
    }

    fun getDraft(context: Context): EmployeeAssetRequestDraft? {
        val storedValue = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyDraft, null)
            ?: return null
        return runCatching { jsonToDraft(JSONObject(storedValue)) }.getOrNull()
    }

    fun clearDraft(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .remove(keyDraft)
            .apply()
    }

    fun saveSubmittedRequest(
        context: Context,
        session: EmployeeSession?,
        category: String,
        assetName: String,
        justification: String,
        priority: String,
        neededBy: LocalDate,
    ): EmployeeAssetRequestRecord {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val requests = getSubmittedRequests(context).toMutableList()
        val nextId = (requests.maxOfOrNull { it.id } ?: 0L) + 1L
        val record = EmployeeAssetRequestRecord(
            id = nextId,
            employeeName = session?.name.orEmpty().ifBlank { "Employee" },
            employeeEmail = session?.email.orEmpty(),
            category = category,
            assetName = assetName,
            justification = justification,
            priority = priority,
            neededBy = neededBy,
            status = "Pending manager review",
            createdAt = LocalDate.now(),
        )
        requests += record
        preferences.edit()
            .putString(keyRequests, JSONArray(requests.map(::requestToJson)).toString())
            .remove(keyDraft)
            .apply()
        return record
    }

    fun getSubmittedRequests(context: Context): List<EmployeeAssetRequestRecord> {
        val storedValue = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyRequests, null)
            ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(storedValue)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    add(jsonToRequest(jsonArray.getJSONObject(index)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun updateRequestStatus(
        context: Context,
        requestId: Long,
        status: String,
    ): Boolean {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val requests = getSubmittedRequests(context).toMutableList()
        val index = requests.indexOfFirst { it.id == requestId }
        if (index == -1) return false

        requests[index] = requests[index].copy(status = status)
        preferences.edit()
            .putString(keyRequests, JSONArray(requests.map(::requestToJson)).toString())
            .apply()
        return true
    }

    fun getRequestsPendingItSupport(context: Context): List<EmployeeAssetRequestRecord> {
        return getSubmittedRequests(context)
            .filter { it.status.equals("Approved", ignoreCase = true) }
            .sortedWith(
                compareByDescending<EmployeeAssetRequestRecord> { it.createdAt }
                    .thenByDescending { it.id }
            )
    }

    fun markRequestFulfilled(
        context: Context,
        requestId: Long,
        assetId: String,
    ): Boolean {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val requests = getSubmittedRequests(context).toMutableList()
        val index = requests.indexOfFirst { it.id == requestId }
        if (index == -1) return false

        requests[index] = requests[index].copy(
            status = "Fulfilled",
            fulfilledAssetId = assetId,
            fulfilledAt = LocalDate.now(),
        )
        preferences.edit()
            .putString(keyRequests, JSONArray(requests.map(::requestToJson)).toString())
            .apply()
        return true
    }

    private fun draftToJson(draft: EmployeeAssetRequestDraft): JSONObject {
        return JSONObject()
            .put("category", draft.category)
            .put("assetName", draft.assetName)
            .put("justification", draft.justification)
            .put("priority", draft.priority)
            .put("neededBy", draft.neededBy?.format(dateFormatter))
    }

    private fun jsonToDraft(json: JSONObject): EmployeeAssetRequestDraft {
        return EmployeeAssetRequestDraft(
            category = json.optString("category").takeIf { it.isNotBlank() },
            assetName = json.optString("assetName"),
            justification = json.optString("justification"),
            priority = json.optString("priority"),
            neededBy = json.optString("neededBy").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        )
    }

    private fun requestToJson(request: EmployeeAssetRequestRecord): JSONObject {
        return JSONObject()
            .put("id", request.id)
            .put("employeeName", request.employeeName)
            .put("employeeEmail", request.employeeEmail)
            .put("category", request.category)
            .put("assetName", request.assetName)
            .put("justification", request.justification)
            .put("priority", request.priority)
            .put("neededBy", request.neededBy.format(dateFormatter))
            .put("status", request.status)
            .put("createdAt", request.createdAt.format(dateFormatter))
            .put("fulfilledAssetId", request.fulfilledAssetId)
            .put("fulfilledAt", request.fulfilledAt?.format(dateFormatter))
    }

    private fun jsonToRequest(json: JSONObject): EmployeeAssetRequestRecord {
        return EmployeeAssetRequestRecord(
            id = json.optLong("id"),
            employeeName = json.optString("employeeName"),
            employeeEmail = json.optString("employeeEmail"),
            category = json.optString("category"),
            assetName = json.optString("assetName"),
            justification = json.optString("justification"),
            priority = json.optString("priority"),
            neededBy = LocalDate.parse(json.getString("neededBy"), dateFormatter),
            status = json.optString("status"),
            createdAt = LocalDate.parse(json.getString("createdAt"), dateFormatter),
            fulfilledAssetId = json.optString("fulfilledAssetId").takeIf { it.isNotBlank() },
            fulfilledAt = json.optString("fulfilledAt").takeIf { it.isNotBlank() }
                ?.let(LocalDate::parse),
        )
    }
}

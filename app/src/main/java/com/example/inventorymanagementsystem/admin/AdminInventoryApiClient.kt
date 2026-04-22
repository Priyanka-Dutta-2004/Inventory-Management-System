package com.example.inventorymanagementsystem.admin

import com.example.inventorymanagementsystem.network.BackendApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class InventoryAsset(
    val id: Long,
    val assetId: String,
    val name: String,
    val category: String,
    val location: String,
    val assignedTo: String?,
    val status: String,
    val condition: String,
    val warrantyEndDate: String?,
    val lastAuditDate: String?,
    val notes: String,
)

data class InventorySummary(
    val availableAssets: Int,
    val allocatedAssets: Int,
    val retiredAssets: Int,
)

data class InventoryAssetRequest(
    val assetId: String,
    val name: String,
    val category: String,
    val location: String,
    val assignedTo: String?,
    val status: String,
    val condition: String,
    val warrantyEndDate: String?,
    val lastAuditDate: String?,
    val notes: String,
)

object AdminInventoryApiClient {
    fun listAssets(
        query: String,
        status: String?,
        onSuccess: (List<InventoryAsset>) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                val httpUrl = "${baseUrl}api/admin/inventory"
                    .toHttpUrl()
                    .newBuilder()
                    .apply {
                        if (query.isNotBlank()) addQueryParameter("query", query)
                        if (!status.isNullOrBlank()) addQueryParameter("status", status)
                    }
                    .build()
                Request.Builder().url(httpUrl).get().build()
            },
            onSuccess = { body -> onSuccess(JSONArray(body).toAssetList()) },
            onError = onError,
        )
    }

    fun getSummary(
        onSuccess: (InventorySummary) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory/summary")
                    .get()
                    .build()
            },
            onSuccess = { body ->
                val json = JSONObject(body)
                onSuccess(
                    InventorySummary(
                        availableAssets = json.optInt("availableAssets"),
                        allocatedAssets = json.optInt("allocatedAssets"),
                        retiredAssets = json.optInt("retiredAssets"),
                    )
                )
            },
            onError = onError,
        )
    }

    fun createAsset(
        request: InventoryAssetRequest,
        onSuccess: (InventoryAsset) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory")
                    .post(BackendApi.jsonBody(request.toJson()))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toAsset()) },
            onError = onError,
        )
    }

    fun updateAsset(
        id: Long,
        request: InventoryAssetRequest,
        onSuccess: (InventoryAsset) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory/$id")
                    .put(BackendApi.jsonBody(request.toJson()))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toAsset()) },
            onError = onError,
        )
    }

    fun updateStatus(
        id: Long,
        status: String,
        onSuccess: (InventoryAsset) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject().put("status", status)
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory/$id/status")
                    .patch(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toAsset()) },
            onError = onError,
        )
    }

    fun transferAsset(
        id: Long,
        location: String,
        assignedTo: String?,
        onSuccess: (InventoryAsset) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject()
            .put("location", location)
            .put("assignedTo", assignedTo)
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory/$id/transfer")
                    .patch(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toAsset()) },
            onError = onError,
        )
    }

    fun bulkUpdateStatus(
        ids: List<Long>,
        status: String,
        onSuccess: (List<InventoryAsset>) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject()
            .put("ids", JSONArray(ids))
            .put("status", status)
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/inventory/bulk-status")
                    .patch(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONArray(body).toAssetList()) },
            onError = onError,
        )
    }

    private fun InventoryAssetRequest.toJson(): JSONObject {
        return JSONObject()
            .put("assetId", assetId)
            .put("name", name)
            .put("category", category)
            .put("location", location)
            .put("assignedTo", assignedTo)
            .put("status", status)
            .put("condition", condition)
            .put("warrantyEndDate", warrantyEndDate)
            .put("lastAuditDate", lastAuditDate)
            .put("notes", notes)
    }

    private fun JSONArray.toAssetList(): List<InventoryAsset> {
        val assets = mutableListOf<InventoryAsset>()
        for (index in 0 until length()) {
            assets += getJSONObject(index).toAsset()
        }
        return assets
    }

    private fun JSONObject.toAsset(): InventoryAsset {
        return InventoryAsset(
            id = optLong("id"),
            assetId = optString("assetId"),
            name = optString("name"),
            category = optString("category"),
            location = optString("location"),
            assignedTo = optString("assignedTo").takeIf { it.isNotBlank() },
            status = optString("status"),
            condition = optString("condition"),
            warrantyEndDate = optString("warrantyEndDate").takeIf { it.isNotBlank() },
            lastAuditDate = optString("lastAuditDate").takeIf { it.isNotBlank() },
            notes = optString("notes"),
        )
    }

}

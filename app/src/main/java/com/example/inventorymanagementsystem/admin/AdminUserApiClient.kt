package com.example.inventorymanagementsystem.admin

import com.example.inventorymanagementsystem.network.BackendApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class AdminUser(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
    val status: String,
)

data class AdminUserSummary(
    val totalUsers: Int,
    val adminUsers: Int,
    val inactiveUsers: Int,
)

data class AdminUserUpsertRequest(
    val name: String,
    val email: String,
    val password: String? = null,
    val role: String,
    val status: String,
)

object AdminUserApiClient {
    fun listUsers(
        query: String,
        onSuccess: (List<AdminUser>) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                val httpUrl = "${baseUrl}api/admin/users"
                    .toHttpUrl()
                    .newBuilder()
                    .apply {
                        if (query.isNotBlank()) {
                            addQueryParameter("query", query)
                        }
                    }
                    .build()
                Request.Builder().url(httpUrl).get().build()
            },
            onSuccess = { body ->
                val users = JSONArray(body).toUserList()
                onSuccess(users)
            },
            onError = onError,
        )
    }

    fun getSummary(
        onSuccess: (AdminUserSummary) -> Unit,
        onError: (String) -> Unit,
    ) {
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/users/summary")
                    .get()
                    .build()
            },
            onSuccess = { body ->
                val json = JSONObject(body)
                onSuccess(
                    AdminUserSummary(
                        totalUsers = json.optInt("totalUsers"),
                        adminUsers = json.optInt("adminUsers"),
                        inactiveUsers = json.optInt("inactiveUsers"),
                    )
                )
            },
            onError = onError,
        )
    }

    fun createUser(
        request: AdminUserUpsertRequest,
        onSuccess: (AdminUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject()
            .put("name", request.name)
            .put("email", request.email)
            .put("password", request.password.orEmpty())
            .put("role", request.role)
            .put("status", request.status)

        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/users")
                    .post(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toUser()) },
            onError = onError,
        )
    }

    fun updateUser(
        userId: Long,
        request: AdminUserUpsertRequest,
        onSuccess: (AdminUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject()
            .put("name", request.name)
            .put("email", request.email)
            .put("role", request.role)
            .put("status", request.status)

        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/users/$userId")
                    .put(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toUser()) },
            onError = onError,
        )
    }

    fun updateStatus(
        userId: Long,
        status: String,
        onSuccess: (AdminUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject().put("status", status)
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/users/$userId/status")
                    .patch(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toUser()) },
            onError = onError,
        )
    }

    fun resetPassword(
        userId: Long,
        newPassword: String,
        onSuccess: (AdminUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject().put("password", newPassword)
        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/admin/users/$userId/password")
                    .patch(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body -> onSuccess(JSONObject(body).toUser()) },
            onError = onError,
        )
    }

    private fun JSONArray.toUserList(): List<AdminUser> {
        val users = mutableListOf<AdminUser>()
        for (index in 0 until length()) {
            users += getJSONObject(index).toUser()
        }
        return users
    }

    private fun JSONObject.toUser(): AdminUser {
        return AdminUser(
            userId = optLong("userId"),
            name = optString("name"),
            email = optString("email"),
            role = optString("role"),
            status = optString("status"),
        )
    }
}

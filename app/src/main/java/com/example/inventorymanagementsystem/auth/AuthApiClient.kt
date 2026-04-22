package com.example.inventorymanagementsystem.auth

import com.example.inventorymanagementsystem.network.BackendApi
import okhttp3.Request
import org.json.JSONObject

data class LoginResponse(
    val message: String,
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
)

object AuthApiClient {
    fun login(
        email: String,
        password: String,
        role: String,
        onSuccess: (LoginResponse) -> Unit,
        onError: (String) -> Unit,
    ) {
        val payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("role", role)

        BackendApi.execute(
            requestFactory = { baseUrl ->
                Request.Builder()
                    .url("${baseUrl}api/auth/login")
                    .post(BackendApi.jsonBody(payload))
                    .build()
            },
            onSuccess = { body ->
                val json = JSONObject(body)
                onSuccess(
                    LoginResponse(
                        message = json.optString("message", "Login successful"),
                        userId = json.optLong("userId"),
                        name = json.optString("name"),
                        email = json.optString("email"),
                        role = json.optString("role"),
                    )
                )
            },
            onError = onError,
        )
    }
}

package com.example.inventorymanagementsystem.auth

import com.example.inventorymanagementsystem.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class LoginResponse(
    val message: String,
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
)

object AuthApiClient {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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

        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_BASE_URL}api/auth/login")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("Unable to reach backend. Check that the server is running.")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        onError(parseError(body))
                        return
                    }

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
                }
            }
        })
    }

    private fun parseError(responseBody: String): String {
        if (responseBody.isBlank()) {
            return "Login failed"
        }

        return runCatching {
            val json = JSONObject(responseBody)
            json.optString("message")
                .takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Login failed"
        }.getOrDefault("Login failed")
    }
}

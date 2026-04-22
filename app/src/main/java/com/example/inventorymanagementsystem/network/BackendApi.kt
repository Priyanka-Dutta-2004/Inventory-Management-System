package com.example.inventorymanagementsystem.network

import com.example.inventorymanagementsystem.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object BackendApi {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedBaseUrl: String? = null

    fun jsonBody(payload: JSONObject) = payload.toString().toRequestBody(jsonMediaType)

    fun execute(
        requestFactory: (String) -> Request,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        resolveBaseUrl(
            onSuccess = { baseUrl ->
                executeRequest(
                    request = requestFactory(baseUrl),
                    onSuccess = onSuccess,
                    onError = { message ->
                        cachedBaseUrl = null
                        onError(message)
                    },
                )
            },
            onError = onError,
        )
    }

    private fun resolveBaseUrl(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        cachedBaseUrl?.let {
            onSuccess(it)
            return
        }

        val candidates = buildCandidateUrls()
        tryResolveCandidate(
            candidates = candidates,
            index = 0,
            onResolved = { resolved ->
                cachedBaseUrl = resolved
                onSuccess(resolved)
            },
            onError = onError,
        )
    }

    private fun tryResolveCandidate(
        candidates: List<String>,
        index: Int,
        onResolved: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (index >= candidates.size) {
            onError("Unable to reach backend. Start the backend or use emulator host 10.0.2.2 / adb reverse.")
            return
        }

        val candidate = candidates[index]
        val healthRequest = Request.Builder()
            .url("${candidate}actuator/health")
            .get()
            .build()

        client.newCall(healthRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                tryResolveCandidate(candidates, index + 1, onResolved, onError)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (it.isSuccessful) {
                        onResolved(candidate)
                    } else {
                        tryResolveCandidate(candidates, index + 1, onResolved, onError)
                    }
                }
            }
        })
    }

    private fun buildCandidateUrls(): List<String> {
        return listOf(
            BuildConfig.BACKEND_BASE_URL,
            "http://10.0.2.2:8080/",
            "http://10.0.3.2:8080/",
            "http://127.0.0.1:8080/",
            "http://localhost:8080/",
        ).map(::normalizeBaseUrl).distinct()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private fun executeRequest(
        request: Request,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
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
                    onSuccess(body)
                }
            }
        })
    }

    private fun parseError(responseBody: String): String {
        if (responseBody.isBlank()) return "Request failed"
        return runCatching {
            val json = JSONObject(responseBody)
            json.optString("message")
                .takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Request failed"
        }.getOrDefault("Request failed")
    }
}

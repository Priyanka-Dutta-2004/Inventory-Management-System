package com.example.inventorymanagementsystem.employee

import android.content.Context

data class EmployeeSession(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String,
)

object EmployeeSessionManager {
    private const val preferencesName = "employee_session"
    private const val keyUserId = "user_id"
    private const val keyName = "name"
    private const val keyEmail = "email"
    private const val keyRole = "role"

    fun saveSession(
        context: Context,
        userId: Long,
        name: String,
        email: String,
        role: String,
    ) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putLong(keyUserId, userId)
            .putString(keyName, name)
            .putString(keyEmail, email)
            .putString(keyRole, role)
            .apply()
    }

    fun getSession(context: Context): EmployeeSession? {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val userId = preferences.getLong(keyUserId, -1L)
        val email = preferences.getString(keyEmail, null)
        val role = preferences.getString(keyRole, null)

        if (userId <= 0L || email.isNullOrBlank() || role.isNullOrBlank()) {
            return null
        }

        val name = preferences.getString(keyName, null).orEmpty().ifBlank { "Employee" }
        return EmployeeSession(
            userId = userId,
            name = name,
            email = email,
            role = role,
        )
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

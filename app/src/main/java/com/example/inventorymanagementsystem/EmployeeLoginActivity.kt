package com.example.inventorymanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.auth.AuthApiClient
import com.example.inventorymanagementsystem.employee.EmployeeDashboardActivity
import com.google.android.material.textfield.TextInputEditText

class EmployeeLoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_login)

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        // Set login button click listener
        loginButton.setOnClickListener {
            performLogin()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Simple validation
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }

        loginButton.isEnabled = false
        AuthApiClient.login(
            email = email,
            password = password,
            role = "EMPLOYEE",
            onSuccess = {
                runOnUiThread {
                    loginButton.isEnabled = true
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                }
            },
            onError = { message ->
                runOnUiThread {
                    loginButton.isEnabled = true
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, EmployeeDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}

package com.example.inventorymanagementsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.manager.ManagerDashboardActivity

class LandingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userBtn = findViewById<Button>(R.id.userBtn)
        val adminBtn = findViewById<Button>(R.id.adminBtn)
        val managerBtn = findViewById<Button>(R.id.managerBtn)

        userBtn.setOnClickListener {
            val intent = Intent(this, EmployeeLoginActivity::class.java)
            startActivity(intent)
        }

        managerBtn.setOnClickListener {
            val intent = Intent(this, ManagerLoginActivity::class.java)
            startActivity(intent)
        }

        adminBtn.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
        }
    }
}

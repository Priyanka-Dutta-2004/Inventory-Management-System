package com.example.inventorymanagementsystem.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.card.MaterialCardView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.LandingPageActivity
import com.example.inventorymanagementsystem.R

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        findViewById<MaterialCardView>(R.id.cardAddItem).setOnClickListener {
            startActivity(Intent(this, AdminAddItemActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardUsers).setOnClickListener {
            startActivity(Intent(this, AdminMaintainUsersActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardInventory).setOnClickListener {
            startActivity(Intent(this, AdminMaintainInventoryActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardReports).setOnClickListener {
            startActivity(Intent(this, AdminReportActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_profile, R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        val intent = Intent(this, LandingPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

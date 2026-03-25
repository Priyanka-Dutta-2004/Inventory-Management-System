package com.example.inventorymanagementsystem.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class ManagerDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manager_dashboard)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        findViewById<MaterialCardView>(R.id.cardAcceptRequest).setOnClickListener {
            startActivity(Intent(this, ManagerAcceptRequestsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardReport).setOnClickListener {
            startActivity(Intent(this, ManagerReportsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardBrowse).setOnClickListener {
            startActivity(Intent(this, ManagerTeamAssetsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMyItems).setOnClickListener {
            startActivity(Intent(this, ManagerTeamAssetsActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

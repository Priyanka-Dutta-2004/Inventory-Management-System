package com.example.inventorymanagementsystem.admin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.LandingPageActivity
import android.widget.PopupMenu
import com.example.inventorymanagementsystem.R

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var totalItemsValue: TextView
    private lateinit var lowStockValue: TextView
    private lateinit var usersValue: TextView
    private lateinit var recentActivityContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 10_000L
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshDashboard()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.admin_menu, popup.menu)
            popup.setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
            popup.show()
        }

        totalItemsValue = findViewById(R.id.totalItemsValue)
        lowStockValue = findViewById(R.id.lowStockValue)
        usersValue = findViewById(R.id.usersValue)
        recentActivityContainer = findViewById(R.id.recentActivityContainer)

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
        findViewById<MaterialCardView>(R.id.cardRequests).setOnClickListener {
            startActivity(Intent(this, AdminAcceptRequestsActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun refreshDashboard() {
        // Fetch server-side summaries and preview recent activity
        AdminReportApiClient.getSummary(onSuccess = { summary ->
            runOnUiThread {
                totalItemsValue.text = summary.totalAssets.toString()
                lowStockValue.text = summary.expiringAssets.toString()
            }
        }, onError = { /* ignore */ })

        AdminUserApiClient.getSummary(onSuccess = { summary ->
            runOnUiThread { usersValue.text = summary.totalUsers.toString() }
        }, onError = { /* ignore */ })

        AdminReportApiClient.previewReport(
            reportType = "INVENTORY_SUMMARY",
            fromDate = null,
            toDate = null,
            department = null,
            onSuccess = { preview ->
                runOnUiThread {
                    recentActivityContainer.removeAllViews()
                    if (preview.rows.isEmpty()) {
                        val empty = layoutInflater.inflate(R.layout.item_employee_recent_activity, recentActivityContainer, false)
                        empty.findViewById<TextView>(R.id.activityTitle).text = "No recent activity"
                        empty.findViewById<TextView>(R.id.activityDescription).text = ""
                        recentActivityContainer.addView(empty)
                    } else {
                        preview.rows.take(5).forEach { row ->
                            val item = layoutInflater.inflate(R.layout.item_employee_recent_activity, recentActivityContainer, false)
                            item.findViewById<TextView>(R.id.activityTitle).text = row
                            item.findViewById<TextView>(R.id.activityDescription).text = ""
                            recentActivityContainer.addView(item)
                        }
                    }
                }
            },
            onError = { /* ignore */ },
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_item -> {
                startActivity(Intent(this, AdminAddItemActivity::class.java))
                true
            }
            R.id.action_maintain_users -> {
                startActivity(Intent(this, AdminMaintainUsersActivity::class.java))
                true
            }
            R.id.action_maintain_inventory -> {
                startActivity(Intent(this, AdminMaintainInventoryActivity::class.java))
                true
            }
            R.id.action_reports -> {
                startActivity(Intent(this, AdminReportActivity::class.java))
                true
            }
            R.id.action_accept_requests -> {
                startActivity(Intent(this, AdminAcceptRequestsActivity::class.java))
                true
            }
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

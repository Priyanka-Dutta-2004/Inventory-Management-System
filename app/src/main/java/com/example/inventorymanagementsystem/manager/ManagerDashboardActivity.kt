package com.example.inventorymanagementsystem.manager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.LandingPageActivity
import android.widget.PopupMenu
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class ManagerDashboardActivity : AppCompatActivity() {
    private lateinit var pendingCountValue: TextView
    private lateinit var assetsCountValue: TextView
    private lateinit var reportsCountValue: TextView
    private lateinit var managerRecentContainer: LinearLayout
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
        setContentView(R.layout.activity_manager_dashboard)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.manager_menu, popup.menu)
            popup.setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
            popup.show()
        }

        pendingCountValue = findViewById(R.id.pendingCountValue)
        assetsCountValue = findViewById(R.id.assetsCountValue)
        reportsCountValue = findViewById(R.id.reportsCountValue)
        managerRecentContainer = findViewById(R.id.managerRecentContainer)

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

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun refreshDashboard() {
        // Local repository provides a quick summary
        val summary = ManagerRepository.buildSummary(this)
        runOnUiThread {
            pendingCountValue.text = summary.pendingRequests.toString()
            assetsCountValue.text = summary.totalAssets.toString()
            reportsCountValue.text = summary.expiringAssets.toString()
        }

        // Preview pending requests as recent activity
        val preview = ManagerRepository.previewReport(this, "PENDING_REQUESTS", null, null, null)
        runOnUiThread {
            managerRecentContainer.removeAllViews()
            if (preview.rows.isEmpty()) {
                val empty = layoutInflater.inflate(R.layout.item_employee_recent_activity, managerRecentContainer, false)
                empty.findViewById<TextView>(R.id.activityTitle).text = "No recent activity"
                empty.findViewById<TextView>(R.id.activityDescription).text = ""
                managerRecentContainer.addView(empty)
            } else {
                preview.rows.take(5).forEach { row ->
                    val item = layoutInflater.inflate(R.layout.item_employee_recent_activity, managerRecentContainer, false)
                    item.findViewById<TextView>(R.id.activityTitle).text = row
                    item.findViewById<TextView>(R.id.activityDescription).text = ""
                    managerRecentContainer.addView(item)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.manager_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_accept_requests -> {
                startActivity(Intent(this, ManagerAcceptRequestsActivity::class.java))
                true
            }
            R.id.action_reports -> {
                startActivity(Intent(this, ManagerReportsActivity::class.java))
                true
            }
            R.id.action_team_assets -> {
                startActivity(Intent(this, ManagerTeamAssetsActivity::class.java))
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

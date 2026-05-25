package com.example.inventorymanagementsystem.employee

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
import androidx.appcompat.widget.Toolbar
import android.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.LandingPageActivity
import com.example.inventorymanagementsystem.R
import com.google.android.material.card.MaterialCardView

class EmployeeDashboardActivity : AppCompatActivity() {
    private lateinit var welcomeText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var myItemsValue: TextView
    private lateinit var requestsValue: TextView
    private lateinit var alertsValue: TextView
    private lateinit var recentRequestsContainer: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 10_000L
    private val refreshRunnable = object : Runnable {
        override fun run() {
            renderDashboard()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v ->
            val popup = PopupMenu(this, v)
            popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
            popup.setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
            popup.show()
        }
        bindViews()
        renderDashboard()

        findViewById<MaterialCardView>(R.id.cardBrowse).setOnClickListener {
            startActivity(Intent(this, EmployeeAssetRequestActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMyItems).setOnClickListener {
            startActivity(Intent(this, EmployeeMyAssetsActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardRequest).setOnClickListener {
            startActivity(Intent(this, EmployeeIssueReportActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardHistory).setOnClickListener {
            startActivity(Intent(this, EmployeeNearExpiryActivity::class.java))
        }

        // Try to find and set maintenance button if it exists
        val maintenanceId = resources.getIdentifier("cardMaintenance", "id", packageName)
        if (maintenanceId != 0) {
            val maintenanceCard = findViewById<MaterialCardView>(maintenanceId)
            maintenanceCard?.setOnClickListener {
                startActivity(Intent(this, EmployeeMaintenanceRequestActivity::class.java))
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        renderDashboard()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_browse_assets -> {
                startActivity(Intent(this, EmployeeAssetRequestActivity::class.java))
                true
            }
            R.id.action_my_items -> {
                startActivity(Intent(this, EmployeeMyAssetsActivity::class.java))
                true
            }
            R.id.action_report_issue -> {
                startActivity(Intent(this, EmployeeIssueReportActivity::class.java))
                true
            }
            R.id.action_view_history -> {
                startActivity(Intent(this, EmployeeNearExpiryActivity::class.java))
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
        EmployeeSessionManager.clearSession(this)
        val intent = Intent(this, LandingPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun bindViews() {
        welcomeText = findViewById(R.id.welcomeText)
        subtitleText = findViewById(R.id.dateText)
        myItemsValue = findViewById(R.id.myItemsValue)
        requestsValue = findViewById(R.id.requestsValue)
        alertsValue = findViewById(R.id.alertsValue)
        recentRequestsContainer = findViewById(R.id.recentRequestsContainer)
    }

    private fun renderDashboard() {
        val session = EmployeeSessionManager.getSession(this)
        val assets = EmployeeAssetRepository.getAssets(session)
        val summary = EmployeeAssetRepository.buildSummary(assets)
        val recentActivity = EmployeeAssetRepository.buildRecentActivity(assets)

        val employeeName = session?.name?.substringBefore(" ")?.ifBlank { null } ?: "Employee"
        welcomeText.text = getString(R.string.employee_dashboard_welcome_format, employeeName)
        subtitleText.text = getString(
            R.string.employee_dashboard_subtitle_format,
            summary.totalAssets,
            summary.attentionAssets,
        )
        myItemsValue.text = summary.totalAssets.toString()
        requestsValue.text = summary.requests.toString()
        alertsValue.text = summary.alerts.toString()

        recentRequestsContainer.removeAllViews()
        recentActivity.forEach { activity ->
            val row = layoutInflater.inflate(R.layout.item_employee_recent_activity, recentRequestsContainer, false)
            row.findViewById<TextView>(R.id.activityTitle).text = activity.title
            row.findViewById<TextView>(R.id.activityDescription).text = activity.description
            recentRequestsContainer.addView(row)
        }
    }
}

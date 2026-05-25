package com.example.inventorymanagementsystem.itsupport

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
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.employee.EmployeeMaintenanceRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class ITSupportDashboardActivity : AppCompatActivity() {

    private lateinit var pendingTicketsValue: TextView
    private lateinit var resolvedTicketsValue: TextView
    private lateinit var recentTicketsContainer: LinearLayout
    private lateinit var cardManualMaintenance: MaterialCardView
    private lateinit var cardScheduledMaintenance: MaterialCardView
    private lateinit var cardAssetFulfillment: MaterialCardView

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
        setContentView(R.layout.activity_itsupport_dashboard)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        val session = ITSupportSessionManager.getSession(this)
        findViewById<TextView>(R.id.welcomeText).text = "Welcome, ${session?.name ?: "IT Support"}"

        pendingTicketsValue = findViewById(R.id.pendingTicketsValue)
        resolvedTicketsValue = findViewById(R.id.resolvedTicketsValue)
        recentTicketsContainer = findViewById(R.id.recentTicketsContainer)
        cardManualMaintenance = findViewById(R.id.cardManualMaintenance)
        cardScheduledMaintenance = findViewById(R.id.cardScheduledMaintenance)
        cardAssetFulfillment = findViewById(R.id.cardAssetFulfillment)

        // Set button listeners
        cardManualMaintenance.setOnClickListener {
            startActivity(Intent(this, ManualMaintenanceActivity::class.java))
        }

        cardScheduledMaintenance.setOnClickListener {
            startActivity(Intent(this, ScheduledMaintenanceActivity::class.java))
        }

        cardAssetFulfillment.setOnClickListener {
            startActivity(Intent(this, ITSupportAssetAllocationActivity::class.java))
        }

        renderDashboard()

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

    private fun renderDashboard() {
        val tickets = EmployeeMaintenanceRepository.getTickets(this)
        val pendingCount = tickets.count { it.status == "Open" }
        val resolvedCount = tickets.count { it.status == "Resolved" }

        runOnUiThread {
            pendingTicketsValue.text = pendingCount.toString()
            resolvedTicketsValue.text = resolvedCount.toString()
            recentTicketsContainer.removeAllViews()

            if (tickets.isEmpty()) {
                val emptyRow = layoutInflater.inflate(R.layout.item_employee_recent_activity, recentTicketsContainer, false)
                emptyRow.findViewById<TextView>(R.id.activityTitle).text = "No maintenance tickets yet"
                emptyRow.findViewById<TextView>(R.id.activityDescription).text = ""
                recentTicketsContainer.addView(emptyRow)
            } else {
                tickets.sortedByDescending { it.id }.take(5).forEach { ticket ->
                    val row = layoutInflater.inflate(R.layout.item_employee_recent_activity, recentTicketsContainer, false)
                    row.findViewById<TextView>(R.id.activityTitle).text = "${ticket.assetName} [${ticket.priority}]"
                    row.findViewById<TextView>(R.id.activityDescription).text = "Reported by ${ticket.employeeName}: ${ticket.description}"
                    row.setOnClickListener {
                        val intent = Intent(this, MaintenanceTicketDetailActivity::class.java)
                        intent.putExtra("ticketId", ticket.id)
                        startActivity(intent)
                    }
                    recentTicketsContainer.addView(row)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.itsupport_menu, menu)
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
        ITSupportSessionManager.clearSession(this)
        val intent = Intent(this, LandingPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

package com.example.inventorymanagementsystem.itsupport

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.employee.EmployeeMaintenanceRepository
import com.example.inventorymanagementsystem.employee.MaintenanceType
import com.google.android.material.appbar.MaterialToolbar

class ManualMaintenanceActivity : AppCompatActivity() {

    private lateinit var ticketsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual_maintenance)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        ticketsContainer = findViewById(R.id.ticketsContainer)

        renderTickets()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        renderTickets()
    }

    private fun renderTickets() {
        val tickets = EmployeeMaintenanceRepository.getTicketsByType(this, MaintenanceType.MANUAL)

        runOnUiThread {
            ticketsContainer.removeAllViews()

            if (tickets.isEmpty()) {
                val emptyView = TextView(this).apply {
                    text = "No manual maintenance requests"
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 32
                    }
                }
                ticketsContainer.addView(emptyView)
            } else {
                tickets.sortedByDescending { it.createdAt }.forEach { ticket ->
                    val row = layoutInflater.inflate(R.layout.item_employee_recent_activity, ticketsContainer, false)
                    row.findViewById<TextView>(R.id.activityTitle).text = "${ticket.assetName} - ${ticket.priority}"
                    row.findViewById<TextView>(R.id.activityDescription).text = "${ticket.employeeName}: ${ticket.description}"
                    row.setOnClickListener {
                        val intent = Intent(this, MaintenanceTicketDetailActivity::class.java)
                        intent.putExtra("ticketId", ticket.id)
                        startActivity(intent)
                    }
                    ticketsContainer.addView(row)
                }
            }
        }
    }
}

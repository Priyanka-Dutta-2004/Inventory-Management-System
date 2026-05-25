package com.example.inventorymanagementsystem.itsupport

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.employee.EmployeeMaintenanceRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

class MaintenanceTicketDetailActivity : AppCompatActivity() {

    private var ticketId: Long = -1
    private lateinit var detailContainer: LinearLayout
    private lateinit var messagesContainer: LinearLayout
    private lateinit var replyEditText: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maintenance_ticket_detail)

        ticketId = intent.getLongExtra("ticketId", -1)
        if (ticketId == -1L) {
            Toast.makeText(this, "Invalid ticket", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        detailContainer = findViewById(R.id.detailContainer)
        messagesContainer = findViewById(R.id.messagesContainer)
        replyEditText = findViewById(R.id.replyEditText)
        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            sendReply()
        }

        renderTicketDetail()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun renderTicketDetail() {
        val ticket = EmployeeMaintenanceRepository.getTicket(this, ticketId)
        if (ticket == null) {
            Toast.makeText(this, "Ticket not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        runOnUiThread {
            // Clear and rebuild detail container
            detailContainer.removeAllViews()

            // Ticket info card
            val detailCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                radius = 12f
                cardElevation = 2f
            }

            val detailLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            // Add detail info
            val titleView = TextView(this).apply {
                text = "${ticket.assetName} [${ticket.priority}]"
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }
            detailLayout.addView(titleView)

            val statusView = TextView(this).apply {
                text = "Status: ${ticket.status}"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }
            detailLayout.addView(statusView)

            val employeeView = TextView(this).apply {
                text = "Reported by: ${ticket.employeeName}"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }
            detailLayout.addView(employeeView)

            val descriptionView = TextView(this).apply {
                text = "Description: ${ticket.description}"
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }
            detailLayout.addView(descriptionView)

            val typeView = TextView(this).apply {
                text = "Type: ${ticket.maintenanceType.name}"
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            detailLayout.addView(typeView)

            detailCard.addView(detailLayout)
            detailContainer.addView(detailCard)

            // Messages section title
            val messagesTitle = TextView(this).apply {
                text = "Messages"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                    topMargin = 8
                }
            }
            detailContainer.addView(messagesTitle)

            // Render messages
            messagesContainer.removeAllViews()
            if (ticket.messages.isEmpty()) {
                val noMessagesView = TextView(this).apply {
                    text = "No messages yet"
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 12
                    }
                }
                messagesContainer.addView(noMessagesView)
            } else {
                ticket.messages.forEach { message ->
                    val messageCard = MaterialCardView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 8
                        }
                        radius = 8f
                        cardElevation = 1f
                    }

                    val messageLayout = LinearLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        orientation = LinearLayout.VERTICAL
                        setPadding(12, 12, 12, 12)
                    }

                    val senderView = TextView(this).apply {
                        text = message.sender
                        textSize = 12f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 4 }
                    }
                    messageLayout.addView(senderView)

                    val contentView = TextView(this).apply {
                        text = message.content
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 4 }
                    }
                    messageLayout.addView(contentView)

                    val timeView = TextView(this).apply {
                        text = message.timestamp.toString()
                        textSize = 11f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    messageLayout.addView(timeView)

                    messageCard.addView(messageLayout)
                    messagesContainer.addView(messageCard)
                }
            }
        }
    }

    private fun sendReply() {
        val replyText = replyEditText.text.toString().trim()
        if (replyText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        val session = ITSupportSessionManager.getSession(this)
        val success = EmployeeMaintenanceRepository.addMessage(
            this,
            ticketId,
            sender = session?.name ?: "IT Support",
            senderEmail = session?.email ?: "support@company.com",
            content = replyText
        )

        if (success) {
            replyEditText.text.clear()
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            renderTicketDetail()
        } else {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.inventorymanagementsystem.admin

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminRequestDetailsActivity : AppCompatActivity() {
    private lateinit var requestTitleText: TextView
    private lateinit var requestOwnerText: TextView
    private lateinit var requestDateText: TextView
    private lateinit var requestNeededByText: TextView
    private lateinit var requestPriorityText: TextView
    private lateinit var requestStatusText: TextView
    private lateinit var requestJustificationText: TextView
    private lateinit var adminCommentInput: TextInputEditText
    private lateinit var approveButton: MaterialButton
    private lateinit var rejectButton: MaterialButton

    private var requestId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_request_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        requestId = intent.getLongExtra(EXTRA_REQUEST_ID, -1L)
        bindViews()
        approveButton.setOnClickListener { saveDecision(true) }
        rejectButton.setOnClickListener { saveDecision(false) }
        renderRequest()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindViews() {
        requestTitleText = findViewById(R.id.requestTitleText)
        requestOwnerText = findViewById(R.id.requestOwnerText)
        requestDateText = findViewById(R.id.requestDateText)
        requestNeededByText = findViewById(R.id.requestNeededByText)
        requestPriorityText = findViewById(R.id.requestPriorityText)
        requestStatusText = findViewById(R.id.requestStatusText)
        requestJustificationText = findViewById(R.id.requestJustificationText)
        adminCommentInput = findViewById(R.id.inputAdminComment)
        approveButton = findViewById(R.id.btnApprove)
        rejectButton = findViewById(R.id.btnReject)
    }

    private fun renderRequest() {
        val request = AdminRequestRepository.getRequest(this, requestId)
        if (request == null) {
            Toast.makeText(this, "Request not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        requestTitleText.text = request.assetName
        requestOwnerText.text = "${request.employeeName} (${request.employeeEmail.ifBlank { "No email" }})"
        requestDateText.text = "Created: ${request.createdAt}"
        requestNeededByText.text = "Needed by: ${request.neededBy}"
        requestPriorityText.text = request.priority
        requestStatusText.text = request.status
        requestJustificationText.text = request.justification

        // Disable buttons when request is no longer waiting for admin
        if (!request.status.equals("Pending admin review", ignoreCase = true)) {
            approveButton.isEnabled = false
            rejectButton.isEnabled = false
        }
    }

    private fun saveDecision(isApproved: Boolean) {
        approveButton.isEnabled = false
        rejectButton.isEnabled = false

        val comment = adminCommentInput.text?.toString()
        val success = if (isApproved) {
            AdminRequestRepository.acceptRequest(this, requestId, comment)
        } else {
            AdminRequestRepository.rejectRequest(this, requestId, comment)
        }

        if (!success) {
            Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
            approveButton.isEnabled = true
            rejectButton.isEnabled = true
            return
        }

        val statusText = if (isApproved) "Approved" else "Rejected"
        Toast.makeText(
            this,
            "Request $statusText successfully. Email notification sent.",
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}

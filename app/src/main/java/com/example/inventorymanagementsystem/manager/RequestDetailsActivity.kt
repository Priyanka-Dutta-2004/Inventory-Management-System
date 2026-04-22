package com.example.inventorymanagementsystem.manager

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

class RequestDetailsActivity : AppCompatActivity() {
    private lateinit var requestTitleText: TextView
    private lateinit var requestOwnerText: TextView
    private lateinit var requestDateText: TextView
    private lateinit var requestNeededByText: TextView
    private lateinit var requestPriorityText: TextView
    private lateinit var requestStatusText: TextView
    private lateinit var requestJustificationText: TextView
    private lateinit var managerCommentInput: TextInputEditText
    private lateinit var approvalHistoryText: TextView
    private lateinit var approveButton: MaterialButton
    private lateinit var rejectButton: MaterialButton

    private var requestId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_request_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        requestId = intent.getLongExtra(EXTRA_REQUEST_ID, -1L)
        bindViews()
        approveButton.setOnClickListener { saveDecision(ManagerRequestDecision.APPROVED) }
        rejectButton.setOnClickListener { saveDecision(ManagerRequestDecision.REJECTED) }
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
        managerCommentInput = findViewById(R.id.inputManagerComment)
        approvalHistoryText = findViewById(R.id.approvalHistoryText)
        approveButton = findViewById(R.id.btnApprove)
        rejectButton = findViewById(R.id.btnReject)
    }

    private fun renderRequest() {
        val request = ManagerRepository.getRequest(this, requestId)
        if (request == null) {
            Toast.makeText(this, R.string.manager_request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        requestTitleText.text = request.assetName
        requestOwnerText.text = getString(
            R.string.manager_request_owner_format,
            request.employeeName,
            request.employeeEmail.ifBlank { getString(R.string.manager_request_no_email) },
        )
        requestDateText.text = getString(
            R.string.manager_request_created_format,
            ManagerRepository.formatRequestDate(request.createdAt),
        )
        requestNeededByText.text = getString(
            R.string.manager_request_needed_by_format,
            ManagerRepository.formatRequestDate(request.neededBy),
        )
        requestPriorityText.text = getString(
            R.string.manager_request_priority_format,
            request.priority,
        )
        requestStatusText.text = request.decision.toStatusLabel()
        requestJustificationText.text = request.justification
        managerCommentInput.setText(request.managerComment.orEmpty())
        approvalHistoryText.text = request.decidedAt?.let {
            getString(
                R.string.manager_request_history_format,
                request.decision.toStatusLabel(),
                ManagerRepository.formatDecisionDateTime(it),
            )
        } ?: getString(R.string.manager_request_history_pending)
    }

    private fun saveDecision(decision: ManagerRequestDecision) {
        approveButton.isEnabled = false
        rejectButton.isEnabled = false

        val updated = ManagerRepository.saveDecision(
            context = this,
            requestId = requestId,
            decision = decision,
            comment = managerCommentInput.text?.toString(),
        )
        if (updated == null) {
            Toast.makeText(this, R.string.manager_request_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(
            this,
            getString(R.string.manager_request_saved_format, decision.toStatusLabel()),
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
    }
}

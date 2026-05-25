package com.example.inventorymanagementsystem.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.employee.EmployeeRequestRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class AdminAcceptRequestsActivity : AppCompatActivity() {
    private val adapter = AdminRequestsAdapter { request ->
        startActivity(
            Intent(this, AdminRequestDetailsActivity::class.java)
                .putExtra(AdminRequestDetailsActivity.EXTRA_REQUEST_ID, request.id)
        )
    }

    private lateinit var requestSearchInput: TextInputEditText
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var pendingCountText: TextView
    private lateinit var approvedCountText: TextView

    private var requests: List<AdminRequestRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_accept_requests)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        requestsRecyclerView.adapter = adapter
        requestSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRequests(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        requests = AdminRequestRepository.getRequests(this)
        renderSummary(requests)
        filterRequests(requestSearchInput.text?.toString().orEmpty())
    }

    private fun bindViews() {
        requestSearchInput = findViewById(R.id.requestSearchInput)
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        pendingCountText = findViewById(R.id.pendingCountText)
        approvedCountText = findViewById(R.id.approvedCountText)
    }

    private fun renderSummary(items: List<AdminRequestRecord>) {
        pendingCountText.text = items.count { it.status == "Pending admin review" }.toString()
        approvedCountText.text = items.count { it.status == "Approved" }.toString()
    }

    private fun filterRequests(query: String) {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            requests
        } else {
            requests.filter { request ->
                request.assetName.contains(normalizedQuery, ignoreCase = true) ||
                    request.employeeName.contains(normalizedQuery, ignoreCase = true) ||
                    request.priority.contains(normalizedQuery, ignoreCase = true) ||
                    request.status.contains(normalizedQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)
        emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        requestsRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}

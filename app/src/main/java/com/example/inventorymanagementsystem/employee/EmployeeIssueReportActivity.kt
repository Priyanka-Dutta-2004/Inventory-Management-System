package com.example.inventorymanagementsystem.employee

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class EmployeeIssueReportActivity : AppCompatActivity() {

    private lateinit var assetSpinner: AutoCompleteTextView
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var priorityGroup: RadioGroup
    private lateinit var submitBtn: Button

    private var selectedAssetId: String? = null
    private var selectedAssetName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_issue_report)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        assetSpinner = findViewById(R.id.assetSpinner)
        descriptionInput = findViewById(R.id.descriptionInput)
        priorityGroup = findViewById(R.id.priorityGroup)
        submitBtn = findViewById(R.id.submitBtn)

        val session = EmployeeSessionManager.getSession(this)
        val assets = EmployeeAssetRepository.getAssets(session)

        val assetNames = assets.map { "${it.name} (${it.assetId})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, assetNames)
        assetSpinner.setAdapter(adapter)

        assetSpinner.setOnItemClickListener { _, _, position, _ ->
            val asset = assets[position]
            selectedAssetId = asset.assetId
            selectedAssetName = asset.name
        }

        submitBtn.setOnClickListener {
            val description = descriptionInput.text.toString().trim()
            val priority = when (priorityGroup.checkedRadioButtonId) {
                R.id.priorityLow -> "Low"
                R.id.priorityHigh -> "High"
                else -> "Medium"
            }

            if (selectedAssetId == null || selectedAssetName == null) {
                Toast.makeText(this, "Please select an asset", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                descriptionInput.error = "Description is required"
                return@setOnClickListener
            }

            EmployeeMaintenanceRepository.saveTicket(
                context = this,
                session = session,
                assetId = selectedAssetId!!,
                assetName = selectedAssetName!!,
                description = description,
                priority = priority
            )

            Toast.makeText(this, "Maintenance request submitted to IT Support", Toast.LENGTH_LONG).show()
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

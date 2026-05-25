package com.example.inventorymanagementsystem.employee

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar

class EmployeeMaintenanceRequestActivity : AppCompatActivity() {

    private lateinit var assetSpinner: Spinner
    private lateinit var descriptionEditText: EditText
    private lateinit var priorityRadioGroup: RadioGroup
    private lateinit var maintenanceTypeRadioGroup: RadioGroup
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_maintenance_request)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        assetSpinner = findViewById(R.id.assetSpinner)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        priorityRadioGroup = findViewById(R.id.priorityRadioGroup)
        maintenanceTypeRadioGroup = findViewById(R.id.maintenanceTypeRadioGroup)
        submitButton = findViewById(R.id.submitButton)

        setupAssetSpinner()

        submitButton.setOnClickListener {
            submitRequest()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupAssetSpinner() {
        val session = EmployeeSessionManager.getSession(this)
        val assets = EmployeeAssetRepository.getAssets(session)
        val assetNames = assets.map { it.name }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, assetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assetSpinner.adapter = adapter
    }

    private fun submitRequest() {
        val assetName = assetSpinner.selectedItem as? String
        val description = descriptionEditText.text.toString().trim()
        val priorityId = priorityRadioGroup.checkedRadioButtonId
        val maintenanceTypeId = maintenanceTypeRadioGroup.checkedRadioButtonId

        if (assetName.isNullOrEmpty()) {
            Toast.makeText(this, "Please select an asset", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        if (priorityId == -1) {
            Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show()
            return
        }

        if (maintenanceTypeId == -1) {
            Toast.makeText(this, "Please select maintenance type", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = findViewById<RadioButton>(priorityId).text.toString()
        val maintenanceType = if (findViewById<RadioButton>(maintenanceTypeId).text.toString() == "Manual") {
            MaintenanceType.MANUAL
        } else {
            MaintenanceType.SCHEDULED
        }

        val session = EmployeeSessionManager.getSession(this)
        val assets = EmployeeAssetRepository.getAssets(session)
        val selectedAsset = assets.firstOrNull { it.name == assetName }

        EmployeeMaintenanceRepository.saveTicket(
            context = this,
            session = session,
            assetId = selectedAsset?.assetId ?: "0",
            assetName = assetName,
            description = description,
            priority = priority,
            maintenanceType = maintenanceType
        )

        Toast.makeText(this, "Maintenance request submitted successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}

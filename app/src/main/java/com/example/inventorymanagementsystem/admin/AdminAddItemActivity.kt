package com.example.inventorymanagementsystem.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import kotlin.random.Random

class AdminAddItemActivity : AppCompatActivity() {
    private lateinit var inputAssetName: TextInputEditText
    private lateinit var inputAssetId: TextInputEditText
    private lateinit var inputCategory: TextInputEditText
    private lateinit var inputSerialNumber: TextInputEditText
    private lateinit var inputCondition: TextInputEditText
    private lateinit var inputDescription: TextInputEditText
    private lateinit var inputPurchaseDate: TextInputEditText
    private lateinit var inputStatus: TextInputEditText
    private lateinit var inputAssignedTo: TextInputEditText
    private lateinit var inputLocation: TextInputEditText
    private lateinit var inputWarrantyStart: TextInputEditText
    private lateinit var inputWarrantyExpiry: TextInputEditText
    private lateinit var inputMaintenancePlan: TextInputEditText
    private lateinit var inputNotes: TextInputEditText
    private lateinit var btnGenerateId: MaterialButton
    private lateinit var btnSaveAsset: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_add_item)

        setupToolbar()
        bindViews()
        setupActions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun bindViews() {
        inputAssetName = findViewById(R.id.inputAssetName)
        inputAssetId = findViewById(R.id.inputAssetId)
        inputCategory = findViewById(R.id.inputCategory)
        inputSerialNumber = findViewById(R.id.inputSerialNumber)
        inputCondition = findViewById(R.id.inputCondition)
        inputDescription = findViewById(R.id.inputDescription)
        inputPurchaseDate = findViewById(R.id.inputPurchaseDate)
        inputStatus = findViewById(R.id.inputStatus)
        inputAssignedTo = findViewById(R.id.inputAssignedTo)
        inputLocation = findViewById(R.id.inputLocation)
        inputWarrantyStart = findViewById(R.id.inputWarrantyStart)
        inputWarrantyExpiry = findViewById(R.id.inputWarrantyExpiry)
        inputMaintenancePlan = findViewById(R.id.inputMaintenancePlan)
        inputNotes = findViewById(R.id.inputNotes)
        btnGenerateId = findViewById(R.id.btnGenerateId)
        btnSaveAsset = findViewById(R.id.btnSaveAsset)
    }

    private fun setupActions() {
        btnGenerateId.setOnClickListener {
            inputAssetId.setText(generateAssetId())
        }
        btnSaveAsset.setOnClickListener {
            saveAsset()
        }
    }

    private fun saveAsset() {
        val assetName = inputAssetName.text?.toString()?.trim().orEmpty()
        val assetId = inputAssetId.text?.toString()?.trim().orEmpty().uppercase()
        val category = inputCategory.text?.toString()?.trim().orEmpty()
        val serialNumber = inputSerialNumber.text?.toString()?.trim().orEmpty()
        val condition = inputCondition.text?.toString()?.trim().orEmpty()
        val description = inputDescription.text?.toString()?.trim().orEmpty()
        val purchaseDate = inputPurchaseDate.text?.toString()?.trim().orEmpty()
        val rawStatus = inputStatus.text?.toString()?.trim().orEmpty()
        val assignedTo = inputAssignedTo.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
        val location = inputLocation.text?.toString()?.trim().orEmpty()
        val warrantyStart = inputWarrantyStart.text?.toString()?.trim().orEmpty()
        val warrantyExpiry = inputWarrantyExpiry.text?.toString()?.trim().orEmpty()
        val maintenancePlan = inputMaintenancePlan.text?.toString()?.trim().orEmpty()
        val notes = inputNotes.text?.toString()?.trim().orEmpty()

        val status = normalizeStatus(rawStatus)
        val validationError = validateForm(
            assetName = assetName,
            assetId = assetId,
            category = category,
            condition = condition,
            location = location,
            status = status,
            purchaseDate = purchaseDate,
            warrantyStart = warrantyStart,
            warrantyExpiry = warrantyExpiry,
        )
        if (validationError != null) {
            showToast(validationError)
            return
        }

        btnSaveAsset.isEnabled = false
        val composedNotes = buildNotes(
            description = description,
            serialNumber = serialNumber,
            purchaseDate = purchaseDate,
            warrantyStart = warrantyStart,
            maintenancePlan = maintenancePlan,
            extraNotes = notes,
        )
        AdminInventoryApiClient.createAsset(
            request = InventoryAssetRequest(
                assetId = assetId,
                name = assetName,
                category = category,
                location = location,
                assignedTo = assignedTo,
                status = status!!,
                condition = condition,
                warrantyEndDate = warrantyExpiry.ifBlank { null },
                lastAuditDate = purchaseDate.ifBlank { null },
                notes = composedNotes,
            ),
            onSuccess = {
                runOnUiThread {
                    btnSaveAsset.isEnabled = true
                    showToast(getString(R.string.admin_inventory_created))
                    finish()
                }
            },
            onError = { message ->
                runOnUiThread {
                    btnSaveAsset.isEnabled = true
                    showToast(message)
                }
            },
        )
    }

    private fun validateForm(
        assetName: String,
        assetId: String,
        category: String,
        condition: String,
        location: String,
        status: String?,
        purchaseDate: String,
        warrantyStart: String,
        warrantyExpiry: String,
    ): String? {
        if (assetName.isBlank()) return getString(R.string.admin_inventory_name_required)
        if (assetId.isBlank()) return getString(R.string.admin_inventory_asset_id_required)
        if (category.isBlank()) return getString(R.string.admin_inventory_category_required)
        if (condition.isBlank()) return getString(R.string.admin_inventory_condition_required)
        if (location.isBlank()) return getString(R.string.admin_inventory_location_required)
        if (status == null) return getString(R.string.admin_add_item_invalid_status)
        if (purchaseDate.isNotBlank() && !isIsoDate(purchaseDate)) return getString(R.string.admin_inventory_date_format_error)
        if (warrantyStart.isNotBlank() && !isIsoDate(warrantyStart)) return getString(R.string.admin_inventory_date_format_error)
        if (warrantyExpiry.isNotBlank() && !isIsoDate(warrantyExpiry)) return getString(R.string.admin_inventory_date_format_error)
        return null
    }

    private fun normalizeStatus(value: String): String? {
        return when (value.trim().replace(' ', '_').uppercase()) {
            "AVAILABLE" -> "AVAILABLE"
            "IN_USE" -> "IN_USE"
            "MAINTENANCE" -> "MAINTENANCE"
            "RETIRED" -> "RETIRED"
            else -> null
        }
    }

    private fun isIsoDate(value: String): Boolean {
        return runCatching { LocalDate.parse(value) }.isSuccess
    }

    private fun buildNotes(
        description: String,
        serialNumber: String,
        purchaseDate: String,
        warrantyStart: String,
        maintenancePlan: String,
        extraNotes: String,
    ): String {
        val sections = mutableListOf<String>()
        if (description.isNotBlank()) sections += description
        if (serialNumber.isNotBlank()) sections += "Serial: $serialNumber"
        if (purchaseDate.isNotBlank()) sections += "Purchase date: $purchaseDate"
        if (warrantyStart.isNotBlank()) sections += "Warranty start: $warrantyStart"
        if (maintenancePlan.isNotBlank()) sections += "Maintenance: $maintenancePlan"
        if (extraNotes.isNotBlank()) sections += extraNotes
        return sections.joinToString(" | ").ifBlank { "New asset registered from admin add item screen." }
    }

    private fun generateAssetId(): String {
        val categoryPrefix = inputCategory.text?.toString()
            ?.trim()
            ?.take(3)
            ?.uppercase()
            ?.padEnd(3, 'X')
            ?.replace(Regex("[^A-Z]"), "X")
            ?: "AST"
        val year = LocalDate.now().year
        val serial = Random.nextInt(100, 1000)
        return "$categoryPrefix-$year-$serial"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

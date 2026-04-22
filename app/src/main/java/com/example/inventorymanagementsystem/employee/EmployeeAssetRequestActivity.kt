package com.example.inventorymanagementsystem.employee

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class EmployeeAssetRequestActivity : AppCompatActivity() {
    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    private lateinit var requestTypeGroup: ChipGroup
    private lateinit var chipLaptop: Chip
    private lateinit var chipMonitor: Chip
    private lateinit var chipAccessory: Chip
    private lateinit var chipSoftware: Chip
    private lateinit var assetNameLayout: TextInputLayout
    private lateinit var assetNameInput: TextInputEditText
    private lateinit var justificationLayout: TextInputLayout
    private lateinit var justificationInput: TextInputEditText
    private lateinit var priorityLayout: TextInputLayout
    private lateinit var priorityInput: MaterialAutoCompleteTextView
    private lateinit var neededByLayout: TextInputLayout
    private lateinit var neededByInput: TextInputEditText
    private lateinit var saveDraftButton: MaterialButton
    private lateinit var submitButton: MaterialButton
    private lateinit var helperText: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusSubtitle: TextView

    private var selectedNeededByDate: LocalDate = LocalDate.now().plusDays(7)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_asset_request)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        setupPriorityDropdown()
        setupNeededByField()
        restoreDraft()
        renderLatestRequestStatus()
        saveDraftButton.setOnClickListener { saveDraft() }
        submitButton.setOnClickListener { submitRequest() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        renderLatestRequestStatus()
    }

    private fun bindViews() {
        requestTypeGroup = findViewById(R.id.requestTypeGroup)
        chipLaptop = findViewById(R.id.chipLaptop)
        chipMonitor = findViewById(R.id.chipMonitor)
        chipAccessory = findViewById(R.id.chipAccessory)
        chipSoftware = findViewById(R.id.chipSoftware)
        assetNameLayout = findViewById(R.id.assetNameLayout)
        assetNameInput = findViewById(R.id.assetNameInput)
        justificationLayout = findViewById(R.id.justificationLayout)
        justificationInput = findViewById(R.id.justificationInput)
        priorityLayout = findViewById(R.id.priorityLayout)
        priorityInput = findViewById(R.id.priorityInput)
        neededByLayout = findViewById(R.id.neededByLayout)
        neededByInput = findViewById(R.id.neededByInput)
        saveDraftButton = findViewById(R.id.saveDraftButton)
        submitButton = findViewById(R.id.submitRequestButton)
        helperText = findViewById(R.id.requestHelperText)
        statusTitle = findViewById(R.id.latestRequestTitle)
        statusSubtitle = findViewById(R.id.latestRequestSubtitle)
    }

    private fun setupPriorityDropdown() {
        val priorities = listOf("Low", "Medium", "High", "Critical")
        priorityInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                priorities,
            )
        )
        if (priorityInput.text.isNullOrBlank()) {
            priorityInput.setText(getString(R.string.employee_request_priority_medium), false)
        }
    }

    private fun setupNeededByField() {
        neededByInput.setText(selectedNeededByDate.format(displayDateFormatter))
        neededByInput.setOnClickListener { showDatePicker() }
        neededByLayout.setEndIconOnClickListener { showDatePicker() }
        neededByInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker()
        }
        neededByInput.keyListener = null
    }

    private fun showDatePicker() {
        val current = selectedNeededByDate
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedNeededByDate = LocalDate.of(year, month + 1, dayOfMonth)
                neededByInput.setText(selectedNeededByDate.format(displayDateFormatter))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth,
        ).show()
    }

    private fun restoreDraft() {
        val draft = EmployeeRequestRepository.getDraft(this)
        if (draft == null) {
            helperText.text = getString(R.string.employee_request_approval_hint)
            return
        }

        setSelectedCategory(draft.category)
        assetNameInput.setText(draft.assetName)
        justificationInput.setText(draft.justification)
        if (draft.priority.isNotBlank()) {
            priorityInput.setText(draft.priority, false)
        }
        draft.neededBy?.let {
            selectedNeededByDate = it
            neededByInput.setText(it.format(displayDateFormatter))
        }
        helperText.text = getString(R.string.employee_request_draft_restored)
    }

    private fun saveDraft() {
        EmployeeRequestRepository.saveDraft(
            context = this,
            draft = EmployeeAssetRequestDraft(
                category = selectedCategory(),
                assetName = assetNameInput.text?.toString().orEmpty().trim(),
                justification = justificationInput.text?.toString().orEmpty().trim(),
                priority = priorityInput.text?.toString().orEmpty().trim(),
                neededBy = selectedNeededByDate,
            ),
        )
        Toast.makeText(this, R.string.employee_request_draft_saved, Toast.LENGTH_SHORT).show()
        helperText.text = getString(R.string.employee_request_draft_saved_hint)
    }

    private fun submitRequest() {
        clearErrors()

        val category = selectedCategory()
        val assetName = assetNameInput.text?.toString().orEmpty().trim()
        val justification = justificationInput.text?.toString().orEmpty().trim()
        val priority = priorityInput.text?.toString().orEmpty().trim()

        if (category == null) {
            Toast.makeText(this, R.string.employee_request_error_type_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (assetName.isBlank()) {
            assetNameLayout.error = getString(R.string.employee_request_error_asset_name_required)
            assetNameInput.requestFocus()
            return
        }
        if (justification.isBlank()) {
            justificationLayout.error = getString(R.string.employee_request_error_justification_required)
            justificationInput.requestFocus()
            return
        }
        if (priority.isBlank()) {
            priorityLayout.error = getString(R.string.employee_request_error_priority_required)
            priorityInput.requestFocus()
            return
        }
        if (selectedNeededByDate.isBefore(LocalDate.now())) {
            neededByLayout.error = getString(R.string.employee_request_error_needed_by_past)
            neededByInput.requestFocus()
            return
        }

        submitButton.isEnabled = false
        val savedRequest = EmployeeRequestRepository.saveSubmittedRequest(
            context = this,
            session = EmployeeSessionManager.getSession(this),
            category = category,
            assetName = assetName,
            justification = justification,
            priority = priority,
            neededBy = selectedNeededByDate,
        )
        submitButton.isEnabled = true

        clearForm()
        renderLatestRequestStatus()
        Toast.makeText(
            this,
            getString(R.string.employee_request_submit_success, savedRequest.assetName),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun renderLatestRequestStatus() {
        val latestRequest = EmployeeRequestRepository.getSubmittedRequests(this)
            .maxByOrNull { it.id }

        if (latestRequest == null) {
            statusTitle.text = getString(R.string.employee_request_latest_empty_title)
            statusSubtitle.text = getString(R.string.employee_request_latest_empty_body)
            return
        }

        statusTitle.text = getString(
            R.string.employee_request_latest_title_format,
            latestRequest.assetName,
        )
        statusSubtitle.text = getString(
            R.string.employee_request_latest_subtitle_format,
            latestRequest.category,
            latestRequest.priority,
            latestRequest.status,
        )
    }

    private fun clearForm() {
        requestTypeGroup.clearCheck()
        assetNameInput.text = null
        justificationInput.text = null
        priorityInput.setText(getString(R.string.employee_request_priority_medium), false)
        selectedNeededByDate = LocalDate.now().plusDays(7)
        neededByInput.setText(selectedNeededByDate.format(displayDateFormatter))
        EmployeeRequestRepository.clearDraft(this)
        helperText.text = getString(R.string.employee_request_submitted_hint)
        clearErrors()
    }

    private fun clearErrors() {
        assetNameLayout.error = null
        justificationLayout.error = null
        priorityLayout.error = null
        neededByLayout.error = null
    }

    private fun selectedCategory(): String? {
        return when (requestTypeGroup.checkedChipId) {
            R.id.chipLaptop -> chipLaptop.text.toString()
            R.id.chipMonitor -> chipMonitor.text.toString()
            R.id.chipAccessory -> chipAccessory.text.toString()
            R.id.chipSoftware -> chipSoftware.text.toString()
            else -> null
        }
    }

    private fun setSelectedCategory(category: String?) {
        when (category?.trim()?.lowercase(Locale.ENGLISH)) {
            chipLaptop.text.toString().lowercase(Locale.ENGLISH) -> chipLaptop.isChecked = true
            chipMonitor.text.toString().lowercase(Locale.ENGLISH) -> chipMonitor.isChecked = true
            chipAccessory.text.toString().lowercase(Locale.ENGLISH) -> chipAccessory.isChecked = true
            chipSoftware.text.toString().lowercase(Locale.ENGLISH) -> chipSoftware.isChecked = true
        }
    }
}

package com.example.inventorymanagementsystem.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textview.MaterialTextView
import java.time.LocalDate

class ManagerReportsActivity : AppCompatActivity() {

    private lateinit var reportTypeInput: MaterialAutoCompleteTextView
    private lateinit var reportFromDateInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var reportToDateInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var reportDepartmentInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnPreviewInput: MaterialButton
    private lateinit var btnExportReport: MaterialButton
    private lateinit var btnRunAuditLog: MaterialButton
    private lateinit var btnRunWarrantyRisk: MaterialButton
    private lateinit var tvTotalAssets: MaterialTextView
    private lateinit var tvLateAssets: MaterialTextView
    private lateinit var tvExpiringAssets: MaterialTextView
    private lateinit var tvMaintenanceAssets: MaterialTextView
    private lateinit var reportPreviewTitle: MaterialTextView
    private lateinit var reportPreviewMeta: MaterialTextView
    private lateinit var reportPreviewBody: MaterialTextView

    private var latestPreview: ManagerReportPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manager_reports)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        setupReportTypeDropdown()
        setupActions()
        renderSummary()
        previewCurrentSelection()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindViews() {
        reportTypeInput = findViewById(R.id.reportTypeInput)
        reportFromDateInput = findViewById(R.id.reportFromDateInput)
        reportToDateInput = findViewById(R.id.reportToDateInput)
        reportDepartmentInput = findViewById(R.id.reportDepartmentInput)
        btnPreviewInput = findViewById(R.id.btnPreviewReport)
        btnExportReport = findViewById(R.id.btnExportReport)
        btnRunAuditLog = findViewById(R.id.btnRunAuditLog)
        btnRunWarrantyRisk = findViewById(R.id.btnRunWarrantyRisk)
        tvTotalAssets = findViewById(R.id.tvTotalAssets)
        tvLateAssets = findViewById(R.id.tvLateAssets)
        tvExpiringAssets = findViewById(R.id.tvExpiringAssets)
        tvMaintenanceAssets = findViewById(R.id.tvMaintenanceAssets)
        reportPreviewTitle = findViewById(R.id.reportPreviewTitle)
        reportPreviewMeta = findViewById(R.id.reportPreviewMeta)
        reportPreviewBody = findViewById(R.id.reportPreviewBody)
    }

    private fun setupReportTypeDropdown() {
        reportTypeInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("Team asset utilization", "Pending requests", "Warranty risk", "Maintenance"),
            )
        )
        if (reportTypeInput.text.isNullOrBlank()) {
            reportTypeInput.setText("Team asset utilization", false)
        }
    }

    private fun setupActions() {
        btnPreviewInput.setOnClickListener { previewCurrentSelection() }
        btnExportReport.setOnClickListener { exportCurrentReport() }
        btnRunAuditLog.setOnClickListener {
            reportTypeInput.setText("Pending requests", false)
            previewCurrentSelection()
        }
        btnRunWarrantyRisk.setOnClickListener {
            reportTypeInput.setText("Warranty risk", false)
            previewCurrentSelection()
        }
    }

    private fun renderSummary() {
        val summary = ManagerRepository.buildSummary(this)
        tvTotalAssets.text = summary.totalAssets.toString()
        tvLateAssets.text = summary.pendingRequests.toString()
        tvExpiringAssets.text = summary.expiringAssets.toString()
        tvMaintenanceAssets.text = summary.maintenanceAssets.toString()
    }

    private fun previewCurrentSelection() {
        val fromDate = parseDate(reportFromDateInput.text?.toString().orEmpty())
        val toDate = parseDate(reportToDateInput.text?.toString().orEmpty())
        if (fromDate == INVALID_DATE || toDate == INVALID_DATE) {
            showToast(getString(R.string.admin_inventory_date_format_error))
            return
        }

        latestPreview = ManagerRepository.previewReport(
            context = this,
            reportType = reportTypeInput.text?.toString().orEmpty(),
            fromDate = fromDate.takeUnless { it == INVALID_DATE },
            toDate = toDate.takeUnless { it == INVALID_DATE },
            department = reportDepartmentInput.text?.toString()?.trim(),
        )

        val preview = latestPreview ?: return
        reportPreviewTitle.text = preview.title
        reportPreviewMeta.text = buildMeta(preview)
        reportPreviewBody.text = preview.rows.joinToString("\n")
    }

    private fun exportCurrentReport() {
        val preview = latestPreview ?: run {
            previewCurrentSelection()
            latestPreview
        } ?: return

        try {
            val title = preview.title
            val meta = buildMeta(preview)
            val body = preview.exportText
            val file = com.example.inventorymanagementsystem.PdfUtil.createPdf(this, title.replace("\\s+".toRegex(), "_"), title, meta, body)
            val uri = com.example.inventorymanagementsystem.PdfUtil.getUriForFile(this, file)
            val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(share, "Share PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(getString(R.string.admin_report_export_copied))
        }
    }

    private fun buildMeta(preview: ManagerReportPreview): String {
        val parts = mutableListOf(getString(R.string.admin_report_generated_on, preview.generatedOn))
        preview.appliedDepartmentFilter?.let { parts += getString(R.string.admin_report_department_filter, it) }
        preview.appliedFromDate?.let { parts += getString(R.string.admin_report_from_date, it) }
        preview.appliedToDate?.let { parts += getString(R.string.admin_report_to_date, it) }
        return parts.joinToString(" | ")
    }

    private fun parseDate(value: String): LocalDate? {
        val normalized = value.trim()
        if (normalized.isBlank()) return null
        return runCatching { LocalDate.parse(normalized) }.getOrElse { INVALID_DATE }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val INVALID_DATE: LocalDate = LocalDate.MIN
    }
}

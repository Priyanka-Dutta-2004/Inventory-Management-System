package com.example.inventorymanagementsystem.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.TextView
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
import com.google.android.material.textfield.TextInputEditText

class AdminReportActivity : AppCompatActivity() {
    private lateinit var reportTypeInput: TextInputEditText
    private lateinit var reportFromDateInput: TextInputEditText
    private lateinit var reportToDateInput: TextInputEditText
    private lateinit var reportDepartmentInput: TextInputEditText
    private lateinit var btnPreviewReport: MaterialButton
    private lateinit var btnExportReport: MaterialButton
    private lateinit var btnRunInventoryUtilization: MaterialButton
    private lateinit var btnRunWarrantyExpiry: MaterialButton
    private lateinit var btnRunAuditLog: MaterialButton
    private lateinit var tvTotalAssets: TextView
    private lateinit var tvOverdueAssets: TextView
    private lateinit var tvMaintenanceAssets: TextView
    private lateinit var tvExpiringAssets: TextView
    private lateinit var reportPreviewTitle: TextView
    private lateinit var reportPreviewMeta: TextView
    private lateinit var reportPreviewBody: TextView

    private var latestPreview: AdminReportPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_report)

        setupToolbar()
        bindViews()
        setupActions()
        loadSummary()
        previewCurrentSelection()

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
        reportTypeInput = findViewById(R.id.reportTypeInput)
        reportFromDateInput = findViewById(R.id.reportFromDateInput)
        reportToDateInput = findViewById(R.id.reportToDateInput)
        reportDepartmentInput = findViewById(R.id.reportDepartmentInput)
        btnPreviewReport = findViewById(R.id.btnPreviewReport)
        btnExportReport = findViewById(R.id.btnExportReport)
        btnRunInventoryUtilization = findViewById(R.id.btnRunInventoryUtilization)
        btnRunWarrantyExpiry = findViewById(R.id.btnRunWarrantyExpiry)
        btnRunAuditLog = findViewById(R.id.btnRunAuditLog)
        tvTotalAssets = findViewById(R.id.tvTotalAssets)
        tvOverdueAssets = findViewById(R.id.tvOverdueAssets)
        tvMaintenanceAssets = findViewById(R.id.tvMaintenanceAssets)
        tvExpiringAssets = findViewById(R.id.tvExpiringAssets)
        reportPreviewTitle = findViewById(R.id.reportPreviewTitle)
        reportPreviewMeta = findViewById(R.id.reportPreviewMeta)
        reportPreviewBody = findViewById(R.id.reportPreviewBody)
    }

    private fun setupActions() {
        btnPreviewReport.setOnClickListener { previewCurrentSelection() }
        btnExportReport.setOnClickListener { exportCurrentReport() }
        btnRunInventoryUtilization.setOnClickListener {
            reportTypeInput.setText("Inventory Summary")
            previewCurrentSelection()
        }
        btnRunWarrantyExpiry.setOnClickListener {
            reportTypeInput.setText("Warranty Expiry")
            previewCurrentSelection()
        }
        btnRunAuditLog.setOnClickListener {
            reportTypeInput.setText("Audit Log Export")
            previewCurrentSelection()
        }
    }

    private fun loadSummary() {
        AdminReportApiClient.getSummary(
            onSuccess = { summary ->
                runOnUiThread {
                    tvTotalAssets.text = summary.totalAssets.toString()
                    tvOverdueAssets.text = summary.overdueAudits.toString()
                    tvMaintenanceAssets.text = summary.maintenanceAssets.toString()
                    tvExpiringAssets.text = summary.expiringAssets.toString()
                }
            },
            onError = { message ->
                runOnUiThread { showToast(message) }
            },
        )
    }

    private fun previewCurrentSelection() {
        val reportType = normalizeReportType(reportTypeInput.text?.toString().orEmpty())
        val fromDate = reportFromDateInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val toDate = reportToDateInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val department = reportDepartmentInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }

        if (!isValidDate(fromDate) || !isValidDate(toDate)) {
            showToast(getString(R.string.admin_inventory_date_format_error))
            return
        }

        btnPreviewReport.isEnabled = false
        AdminReportApiClient.previewReport(
            reportType = reportType,
            fromDate = fromDate,
            toDate = toDate,
            department = department,
            onSuccess = { preview ->
                runOnUiThread {
                    btnPreviewReport.isEnabled = true
                    latestPreview = preview
                    reportPreviewTitle.text = preview.title
                    reportPreviewMeta.text = buildPreviewMeta(preview)
                    reportPreviewBody.text = preview.rows.joinToString("\n")
                }
            },
            onError = { message ->
                runOnUiThread {
                    btnPreviewReport.isEnabled = true
                    showToast(message)
                }
            },
        )
    }

    private fun exportCurrentReport() {
        val preview = latestPreview
        if (preview == null) {
            previewCurrentSelection()
            return
        }

        getSystemService<ClipboardManager>()?.setPrimaryClip(
            ClipData.newPlainText(preview.title, preview.exportText)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_report_export_title, preview.title))
            .setMessage(preview.exportText)
            .setPositiveButton(R.string.admin_report_export_done, null)
            .show()
        showToast(getString(R.string.admin_report_export_copied))
    }

    private fun buildPreviewMeta(preview: AdminReportPreview): String {
        val parts = mutableListOf<String>()
        parts += getString(R.string.admin_report_generated_on, preview.generatedOn)
        preview.appliedFromDate?.let { parts += getString(R.string.admin_report_from_date, it) }
        preview.appliedToDate?.let { parts += getString(R.string.admin_report_to_date, it) }
        preview.appliedDepartmentFilter?.let { parts += getString(R.string.admin_report_department_filter, it) }
        return parts.joinToString(" | ")
    }

    private fun normalizeReportType(value: String): String {
        return when (value.trim().replace(' ', '_').uppercase()) {
            "WARRANTY_EXPIRY" -> "WARRANTY_EXPIRY"
            "AUDIT_LOG_EXPORT", "AUDIT_COMPLIANCE" -> "AUDIT_LOG_EXPORT"
            else -> "INVENTORY_SUMMARY"
        }
    }

    private fun isValidDate(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        return runCatching { java.time.LocalDate.parse(value) }.isSuccess
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

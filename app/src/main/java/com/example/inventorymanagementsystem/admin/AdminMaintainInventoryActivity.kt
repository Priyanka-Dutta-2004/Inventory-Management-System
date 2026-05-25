package com.example.inventorymanagementsystem.admin

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.employee.EmployeeRequestRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AdminMaintainInventoryActivity : AppCompatActivity() {
    private lateinit var searchInventoryInput: TextInputEditText
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var recyclerInventory: RecyclerView
    private lateinit var loadingIndicator: View
    private lateinit var emptyStateText: TextView
    private lateinit var tvAvailableAssets: TextView
    private lateinit var tvAllocatedAssets: TextView
    private lateinit var tvRetiredAssets: TextView
    private lateinit var inventoryAdapter: InventoryAdapter

    private val mainHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var latestRequestId = 0
    private var selectedStatusFilter: String? = null
    private var currentAssets: List<InventoryAsset> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_maintain_inventory)

        setupToolbar()
        bindViews()
        setupRecyclerView()
        setupFilters()
        setupActions()
        loadDashboard(showLoader = true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        searchRunnable?.let(mainHandler::removeCallbacks)
        super.onDestroy()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun bindViews() {
        searchInventoryInput = findViewById(R.id.searchInventoryInput)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        recyclerInventory = findViewById(R.id.recyclerInventory)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyStateText = findViewById(R.id.emptyStateText)
        tvAvailableAssets = findViewById(R.id.tvAvailableAssets)
        tvAllocatedAssets = findViewById(R.id.tvAllocatedAssets)
        tvRetiredAssets = findViewById(R.id.tvRetiredAssets)
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter(
            onCheckIn = { asset -> updateStatus(asset, "AVAILABLE") },
            onEdit = { asset -> showAssetDialog(asset) },
            onTransfer = { asset -> showTransferDialog(asset) },
            onRetire = { asset -> updateStatus(asset, "RETIRED") },
            onUpdateStatus = { asset -> showStatusDialog(asset) },
        )
        recyclerInventory.layoutManager = LinearLayoutManager(this)
        recyclerInventory.adapter = inventoryAdapter
    }

    private fun setupFilters() {
        chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedStatusFilter = when (checkedIds.firstOrNull()) {
                R.id.chipAvailable -> "AVAILABLE"
                R.id.chipInUse -> "IN_USE"
                R.id.chipMaintenance -> "MAINTENANCE"
                R.id.chipRetired -> "RETIRED"
                else -> null
            }
            loadAssets(showLoader = true)
        }
    }

    private fun setupActions() {
        findViewById<MaterialButton>(R.id.btnAddInventoryItem).setOnClickListener {
            showAssetDialog()
        }
        findViewById<MaterialButton>(R.id.btnBulkUpdate).setOnClickListener {
            showBulkUpdateDialog()
        }
        searchInventoryInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let(mainHandler::removeCallbacks)
                searchRunnable = Runnable { loadAssets(showLoader = false) }
                mainHandler.postDelayed(searchRunnable!!, 300L)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun loadDashboard(showLoader: Boolean) {
        loadAssets(showLoader)
        loadSummary()
    }

    private fun loadAssets(showLoader: Boolean) {
        if (showLoader) loadingIndicator.visibility = View.VISIBLE
        val requestId = ++latestRequestId
        AdminInventoryApiClient.listAssets(
            query = searchInventoryInput.text?.toString()?.trim().orEmpty(),
            status = selectedStatusFilter,
            onSuccess = { assets ->
                runOnUiThread {
                    if (requestId != latestRequestId || isFinishing || isDestroyed) return@runOnUiThread
                    currentAssets = assets
                    loadingIndicator.visibility = View.GONE
                    inventoryAdapter.submitList(assets)
                    emptyStateText.visibility = if (assets.isEmpty()) View.VISIBLE else View.GONE
                }
            },
            onError = { message ->
                runOnUiThread {
                    if (requestId != latestRequestId || isFinishing || isDestroyed) return@runOnUiThread
                    loadingIndicator.visibility = View.GONE
                    emptyStateText.visibility = if (inventoryAdapter.itemCount == 0) View.VISIBLE else View.GONE
                    showToast(message)
                }
            },
        )
    }

    private fun loadSummary() {
        AdminInventoryApiClient.getSummary(
            onSuccess = { summary ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    tvAvailableAssets.text = summary.availableAssets.toString()
                    tvAllocatedAssets.text = summary.allocatedAssets.toString()
                    tvRetiredAssets.text = summary.retiredAssets.toString()
                }
            },
            onError = { message ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    showToast(message)
                }
            },
        )
    }

    private fun showAssetDialog(existingAsset: InventoryAsset? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inventory_asset_form, null)
        val assetIdInput = dialogView.findViewById<TextInputEditText>(R.id.etAssetId)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.etAssetName)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.etCategory)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.etLocation)
        val assignedToInput = dialogView.findViewById<TextInputEditText>(R.id.etAssignedTo)
        val conditionInput = dialogView.findViewById<TextInputEditText>(R.id.etCondition)
        val warrantyInput = dialogView.findViewById<TextInputEditText>(R.id.etWarrantyDate)
        val auditInput = dialogView.findViewById<TextInputEditText>(R.id.etLastAuditDate)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.etNotes)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinnerInventoryStatus)

        val statuses = listOf("AVAILABLE", "IN_USE", "MAINTENANCE", "RETIRED")
        statusSpinner.adapter = buildSpinnerAdapter(statuses)

        existingAsset?.let { asset ->
            assetIdInput.setText(asset.assetId)
            nameInput.setText(asset.name)
            categoryInput.setText(asset.category)
            locationInput.setText(asset.location)
            assignedToInput.setText(asset.assignedTo.orEmpty())
            conditionInput.setText(asset.condition)
            warrantyInput.setText(asset.warrantyEndDate.orEmpty())
            auditInput.setText(asset.lastAuditDate.orEmpty())
            notesInput.setText(asset.notes)
            statusSpinner.setSelection(statuses.indexOf(asset.status).coerceAtLeast(0))
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existingAsset == null) R.string.admin_inventory_add_dialog_title else R.string.admin_inventory_edit_dialog_title)
            .setView(dialogView)
            .setNegativeButton(R.string.admin_inventory_cancel, null)
            .setPositiveButton(if (existingAsset == null) R.string.admin_inventory_save else R.string.admin_inventory_update, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val request = InventoryAssetRequest(
                    assetId = assetIdInput.text?.toString()?.trim().orEmpty(),
                    name = nameInput.text?.toString()?.trim().orEmpty(),
                    category = categoryInput.text?.toString()?.trim().orEmpty(),
                    location = locationInput.text?.toString()?.trim().orEmpty(),
                    assignedTo = assignedToInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
                    status = statusSpinner.selectedItem?.toString().orEmpty(),
                    condition = conditionInput.text?.toString()?.trim().orEmpty(),
                    warrantyEndDate = warrantyInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
                    lastAuditDate = auditInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
                    notes = notesInput.text?.toString()?.trim().orEmpty(),
                )

                val validationError = validateAssetRequest(request)
                if (validationError != null) {
                    showToast(validationError)
                    return@setOnClickListener
                }

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                if (existingAsset == null) {
                    AdminInventoryApiClient.createAsset(
                        request = request,
                        onSuccess = {
                            runOnUiThread {
                                dialog.dismiss()
                                loadDashboard(showLoader = true)
                                showToast(getString(R.string.admin_inventory_created))
                            }
                        },
                        onError = { message ->
                            runOnUiThread {
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                showToast(message)
                            }
                        },
                    )
                } else {
                    AdminInventoryApiClient.updateAsset(
                        id = existingAsset.id,
                        request = request,
                        onSuccess = {
                            runOnUiThread {
                                dialog.dismiss()
                                loadDashboard(showLoader = true)
                                showToast(getString(R.string.admin_inventory_updated))
                            }
                        },
                        onError = { message ->
                            runOnUiThread {
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                showToast(message)
                            }
                        },
                    )
                }
            }
        }
        dialog.show()
    }

    private fun showTransferDialog(asset: InventoryAsset) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_inventory_transfer, null)
        val locationInput = dialogView.findViewById<TextInputEditText>(R.id.etTransferLocation)
        val assignedToInput = dialogView.findViewById<TextInputEditText>(R.id.etTransferAssignee)
        val assignedUntilInput = dialogView.findViewById<TextInputEditText>(R.id.etAssignedUntil)
        locationInput.setText(asset.location)
        assignedToInput.setText(asset.assignedTo.orEmpty())

        var selectedAssignedUntil: LocalDate? = null
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        assignedUntilInput.setOnClickListener {
            val now = LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedAssignedUntil = LocalDate.of(year, month + 1, dayOfMonth)
                    assignedUntilInput.setText(selectedAssignedUntil?.format(isoFormatter))
                },
                now.year,
                now.monthValue - 1,
                now.dayOfMonth,
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_inventory_transfer_title, asset.assetId))
            .setView(dialogView)
            .setNegativeButton(R.string.admin_inventory_cancel, null)
            .setPositiveButton(R.string.admin_inventory_transfer_action, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val location = locationInput.text?.toString()?.trim().orEmpty()
                val assignedTo = assignedToInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
                val assignedUntilStr = selectedAssignedUntil?.format(isoFormatter)

                if (location.isBlank()) {
                    showToast(getString(R.string.admin_inventory_location_required))
                    return@setOnClickListener
                }

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                AdminInventoryApiClient.transferAsset(
                    id = asset.id,
                    location = location,
                    assignedTo = assignedTo,
                    onSuccess = { transferredAsset ->
                        // If admin specified an "until" date, append it to asset notes and update the asset
                        if (!assignedUntilStr.isNullOrBlank()) {
                            val newNotes = (transferredAsset.notes + "\nAssigned until: $assignedUntilStr").trim()
                            val request = InventoryAssetRequest(
                                assetId = transferredAsset.assetId,
                                name = transferredAsset.name,
                                category = transferredAsset.category,
                                location = transferredAsset.location,
                                assignedTo = transferredAsset.assignedTo,
                                status = transferredAsset.status,
                                condition = transferredAsset.condition,
                                warrantyEndDate = transferredAsset.warrantyEndDate,
                                lastAuditDate = transferredAsset.lastAuditDate,
                                notes = newNotes,
                            )
                            AdminInventoryApiClient.updateAsset(
                                id = transferredAsset.id,
                                request = request,
                                onSuccess = {
                                    runOnUiThread {
                                        dialog.dismiss()
                                        loadDashboard(showLoader = true)
                                        showToast(getString(R.string.admin_inventory_transfer_success))
                                        // Mark manager requests as assigned for this employee
                                        if (!assignedTo.isNullOrBlank()) {
                                            val submitted = EmployeeRequestRepository.getSubmittedRequests(this)
                                            submitted.filter { it.employeeName.equals(assignedTo, ignoreCase = true) && (it.status.equals("Approved", ignoreCase = true) || it.status.equals("Pending admin review", ignoreCase = true)) }
                                                .forEach { req ->
                                                    EmployeeRequestRepository.updateRequestStatus(this, req.id, "Assigned until $assignedUntilStr")
                                                }
                                        }
                                    }
                                },
                                onError = { message ->
                                    runOnUiThread {
                                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                        showToast(message)
                                    }
                                }
                            )
                        } else {
                            runOnUiThread {
                                dialog.dismiss()
                                loadDashboard(showLoader = true)
                                showToast(getString(R.string.admin_inventory_transfer_success))
                                if (!assignedTo.isNullOrBlank()) {
                                    val submitted = EmployeeRequestRepository.getSubmittedRequests(this)
                                    submitted.filter { it.employeeName.equals(assignedTo, ignoreCase = true) && (it.status.equals("Approved", ignoreCase = true) || it.status.equals("Pending admin review", ignoreCase = true)) }
                                        .forEach { req ->
                                            EmployeeRequestRepository.updateRequestStatus(this, req.id, "Assigned")
                                        }
                                }
                            }
                        }
                    },
                    onError = { message ->
                        runOnUiThread {
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            showToast(message)
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    private fun showStatusDialog(asset: InventoryAsset) {
        val statuses = listOf("AVAILABLE", "IN_USE", "MAINTENANCE", "RETIRED")
        val currentIndex = statuses.indexOf(asset.status).coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_inventory_status_dialog_title, asset.assetId))
            .setSingleChoiceItems(statuses.toTypedArray(), currentIndex) { dialog, which ->
                val nextStatus = statuses[which]
                dialog.dismiss()
                updateStatus(asset, nextStatus)
            }
            .setNegativeButton(R.string.admin_inventory_cancel, null)
            .show()
    }

    private fun showBulkUpdateDialog() {
        if (currentAssets.isEmpty()) {
            showToast(getString(R.string.admin_inventory_no_assets_for_bulk_update))
            return
        }

        val statuses = listOf("AVAILABLE", "IN_USE", "MAINTENANCE", "RETIRED")
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_inventory_bulk_update_title, currentAssets.size))
            .setItems(statuses.toTypedArray()) { _, which ->
                val nextStatus = statuses[which]
                AdminInventoryApiClient.bulkUpdateStatus(
                    ids = currentAssets.map { it.id },
                    status = nextStatus,
                    onSuccess = {
                        runOnUiThread {
                            loadDashboard(showLoader = true)
                            showToast(getString(R.string.admin_inventory_bulk_update_success, nextStatus))
                        }
                    },
                    onError = { message ->
                        runOnUiThread { showToast(message) }
                    },
                )
            }
            .setNegativeButton(R.string.admin_inventory_cancel, null)
            .show()
    }

    private fun updateStatus(asset: InventoryAsset, nextStatus: String) {
        AdminInventoryApiClient.updateStatus(
            id = asset.id,
            status = nextStatus,
            onSuccess = {
                runOnUiThread {
                    loadDashboard(showLoader = true)
                    showToast(getString(R.string.admin_inventory_status_updated, nextStatus))
                }
            },
            onError = { message ->
                runOnUiThread { showToast(message) }
            },
        )
    }

    private fun validateAssetRequest(request: InventoryAssetRequest): String? {
        if (request.assetId.isBlank()) return getString(R.string.admin_inventory_asset_id_required)
        if (request.name.isBlank()) return getString(R.string.admin_inventory_name_required)
        if (request.category.isBlank()) return getString(R.string.admin_inventory_category_required)
        if (request.location.isBlank()) return getString(R.string.admin_inventory_location_required)
        if (request.condition.isBlank()) return getString(R.string.admin_inventory_condition_required)
        if (request.notes.isBlank()) return getString(R.string.admin_inventory_notes_required)
        if (!request.warrantyEndDate.isNullOrBlank() && !isIsoDate(request.warrantyEndDate)) {
            return getString(R.string.admin_inventory_date_format_error)
        }
        if (!request.lastAuditDate.isNullOrBlank() && !isIsoDate(request.lastAuditDate)) {
            return getString(R.string.admin_inventory_date_format_error)
        }
        return null
    }

    private fun isIsoDate(value: String): Boolean {
        return runCatching { java.time.LocalDate.parse(value) }.isSuccess
    }

    private fun buildSpinnerAdapter(options: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private class InventoryAdapter(
    private val onCheckIn: (InventoryAsset) -> Unit,
    private val onEdit: (InventoryAsset) -> Unit,
    private val onTransfer: (InventoryAsset) -> Unit,
    private val onRetire: (InventoryAsset) -> Unit,
    private val onUpdateStatus: (InventoryAsset) -> Unit,
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {
    private val items = mutableListOf<InventoryAsset>()

    fun submitList(assets: List<InventoryAsset>) {
        items.clear()
        items.addAll(assets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory_record, parent, false)
        return InventoryViewHolder(view, onCheckIn, onEdit, onTransfer, onRetire, onUpdateStatus)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InventoryViewHolder(
        itemView: View,
        private val onCheckIn: (InventoryAsset) -> Unit,
        private val onEdit: (InventoryAsset) -> Unit,
        private val onTransfer: (InventoryAsset) -> Unit,
        private val onRetire: (InventoryAsset) -> Unit,
        private val onUpdateStatus: (InventoryAsset) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvAssetName)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvAssetMeta)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvAssetDetails)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipAssetStatus)
        private val btnPrimary: MaterialButton = itemView.findViewById(R.id.btnPrimaryAction)
        private val btnSecondary: MaterialButton = itemView.findViewById(R.id.btnSecondaryAction)
        private val btnTertiary: MaterialButton = itemView.findViewById(R.id.btnTertiaryAction)

        fun bind(asset: InventoryAsset) {
            tvName.text = asset.name
            val assigneeLabel = asset.assignedTo ?: asset.location
            tvMeta.text = itemView.context.getString(R.string.admin_inventory_meta_format, asset.assetId, assigneeLabel)
            tvDetails.text = itemView.context.getString(
                R.string.admin_inventory_details_format,
                asset.warrantyEndDate ?: "-",
                asset.lastAuditDate ?: "-",
                asset.condition,
                asset.notes,
            )

            chipStatus.text = asset.status.replace('_', ' ')
            val statusColor = when (asset.status) {
                "AVAILABLE" -> R.color.success
                "IN_USE" -> R.color.accent
                "MAINTENANCE" -> R.color.warning
                else -> R.color.danger
            }
            chipStatus.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, R.color.surface_light)
            )
            chipStatus.chipStrokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, R.color.border_soft)
            )
            chipStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

            when (asset.status) {
                "MAINTENANCE" -> {
                    btnPrimary.text = itemView.context.getString(R.string.admin_inventory_update_status)
                    btnPrimary.setOnClickListener { onUpdateStatus(asset) }
                    btnSecondary.text = itemView.context.getString(R.string.admin_inventory_edit)
                    btnSecondary.setOnClickListener { onEdit(asset) }
                    btnTertiary.text = itemView.context.getString(R.string.admin_inventory_retire)
                    btnTertiary.setOnClickListener { onRetire(asset) }
                }
                else -> {
                    btnPrimary.text = itemView.context.getString(R.string.admin_inventory_check_in)
                    btnPrimary.setOnClickListener { onCheckIn(asset) }
                    btnSecondary.text = itemView.context.getString(R.string.admin_inventory_edit)
                    btnSecondary.setOnClickListener { onEdit(asset) }
                    btnTertiary.text = itemView.context.getString(R.string.admin_inventory_transfer)
                    btnTertiary.setOnClickListener { onTransfer(asset) }
                }
            }

            if (asset.status == "RETIRED") {
                btnPrimary.text = itemView.context.getString(R.string.admin_inventory_update_status)
                btnPrimary.setOnClickListener { onUpdateStatus(asset) }
                btnTertiary.text = itemView.context.getString(R.string.admin_inventory_transfer)
                btnTertiary.setOnClickListener { onTransfer(asset) }
            }

            btnTertiary.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
            btnTertiary.setTextColor(Color.WHITE)
        }
    }
}

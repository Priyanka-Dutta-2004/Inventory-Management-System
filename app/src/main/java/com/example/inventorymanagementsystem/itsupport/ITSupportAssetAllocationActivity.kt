package com.example.inventorymanagementsystem.itsupport

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.example.inventorymanagementsystem.admin.AdminInventoryApiClient
import com.example.inventorymanagementsystem.admin.InventoryAsset
import com.example.inventorymanagementsystem.employee.EmployeeAssetRequestRecord
import com.example.inventorymanagementsystem.employee.EmployeeRequestRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class ITSupportAssetAllocationActivity : AppCompatActivity() {
    private lateinit var searchInventoryInput: TextInputEditText
    private lateinit var inventoryRecyclerView: RecyclerView
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var emptyInventoryText: TextView
    private lateinit var emptyRequestsText: TextView
    private lateinit var availableInventoryCountText: TextView
    private lateinit var pendingRequestsCountText: TextView

    private val inventoryAdapter = InventoryAdapter()
    private val requestAdapter = RequestAdapter(onAssignAsset = ::showAssignAssetDialog)

    private var allInventory: List<InventoryAsset> = emptyList()
    private var pendingRequests: List<EmployeeAssetRequestRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_itsupport_asset_allocation)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        setupLists()
        setupSearch()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        loadInventory()
        loadPendingRequests()
    }

    private fun bindViews() {
        searchInventoryInput = findViewById(R.id.searchInventoryInput)
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView)
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView)
        emptyInventoryText = findViewById(R.id.emptyInventoryText)
        emptyRequestsText = findViewById(R.id.emptyRequestsText)
        availableInventoryCountText = findViewById(R.id.availableInventoryCountText)
        pendingRequestsCountText = findViewById(R.id.pendingRequestsCountText)
    }

    private fun setupLists() {
        inventoryRecyclerView.layoutManager = LinearLayoutManager(this)
        inventoryRecyclerView.adapter = inventoryAdapter

        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        requestsRecyclerView.adapter = requestAdapter
    }

    private fun setupSearch() {
        searchInventoryInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterInventory(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun loadInventory() {
        AdminInventoryApiClient.listAssets(
            query = "",
            status = null,
            onSuccess = { assets ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    allInventory = assets
                    availableInventoryCountText.text = assets.count { it.status == "AVAILABLE" }.toString()
                    filterInventory(searchInventoryInput.text?.toString().orEmpty())
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

    private fun loadPendingRequests() {
        pendingRequests = EmployeeRequestRepository.getRequestsPendingItSupport(this)
        pendingRequestsCountText.text = pendingRequests.size.toString()
        requestAdapter.submitList(pendingRequests)
        emptyRequestsText.visibility = if (pendingRequests.isEmpty()) View.VISIBLE else View.GONE
        requestsRecyclerView.visibility = if (pendingRequests.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun filterInventory(query: String) {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            allInventory
        } else {
            allInventory.filter { asset ->
                asset.assetId.contains(normalizedQuery, ignoreCase = true) ||
                    asset.name.contains(normalizedQuery, ignoreCase = true) ||
                    asset.category.contains(normalizedQuery, ignoreCase = true) ||
                    asset.location.contains(normalizedQuery, ignoreCase = true) ||
                    asset.status.contains(normalizedQuery, ignoreCase = true)
            }
        }
        inventoryAdapter.submitList(filtered)
        emptyInventoryText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        inventoryRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAssignAssetDialog(request: EmployeeAssetRequestRecord) {
        val availableAssets = allInventory.filter { it.status == "AVAILABLE" }
        if (availableAssets.isEmpty()) {
            showToast(getString(R.string.it_support_no_available_assets))
            return
        }

        val preferred = availableAssets.filter {
            it.name.contains(request.assetName, ignoreCase = true) ||
                request.assetName.contains(it.name, ignoreCase = true)
        }
        val candidates = if (preferred.isNotEmpty()) preferred else availableAssets
        val labels = candidates.map { "${it.assetId} | ${it.name} | ${it.location}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.it_support_assign_dialog_title, request.assetName))
            .setItems(labels) { _, which ->
                assignAssetToEmployee(request, candidates[which])
            }
            .setNegativeButton(R.string.admin_inventory_cancel, null)
            .show()
    }

    private fun assignAssetToEmployee(request: EmployeeAssetRequestRecord, asset: InventoryAsset) {
        val assignee = request.employeeName.ifBlank { request.employeeEmail }.ifBlank { "Employee" }
        AdminInventoryApiClient.transferAsset(
            id = asset.id,
            location = asset.location,
            assignedTo = assignee,
            onSuccess = {
                val saved = EmployeeRequestRepository.markRequestFulfilled(
                    context = this,
                    requestId = request.id,
                    assetId = asset.assetId,
                )
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (!saved) {
                        showToast(getString(R.string.it_support_request_update_failed))
                        return@runOnUiThread
                    }
                    showToast(getString(R.string.it_support_assign_success, asset.assetId, request.employeeName))
                    loadInventory()
                    loadPendingRequests()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private class InventoryAdapter : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {
    private val items = mutableListOf<InventoryAsset>()

    fun submitList(data: List<InventoryAsset>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itsupport_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.assetNameText)
        private val metaText: TextView = itemView.findViewById(R.id.assetMetaText)
        private val statusText: TextView = itemView.findViewById(R.id.assetStatusText)

        fun bind(item: InventoryAsset) {
            nameText.text = item.name
            metaText.text = itemView.context.getString(
                R.string.it_support_inventory_meta,
                item.assetId,
                item.category,
                item.location,
            )
            statusText.text = item.status.replace('_', ' ')
        }
    }
}

private class RequestAdapter(
    private val onAssignAsset: (EmployeeAssetRequestRecord) -> Unit,
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {
    private val items = mutableListOf<EmployeeAssetRequestRecord>()

    fun submitList(data: List<EmployeeAssetRequestRecord>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itsupport_request, parent, false)
        return RequestViewHolder(view, onAssignAsset)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class RequestViewHolder(
        itemView: View,
        private val onAssignAsset: (EmployeeAssetRequestRecord) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val assetNameText: TextView = itemView.findViewById(R.id.requestAssetNameText)
        private val employeeText: TextView = itemView.findViewById(R.id.requestEmployeeText)
        private val metaText: TextView = itemView.findViewById(R.id.requestMetaText)
        private val assignButton: MaterialButton = itemView.findViewById(R.id.assignAssetButton)

        fun bind(item: EmployeeAssetRequestRecord) {
            assetNameText.text = item.assetName
            employeeText.text = itemView.context.getString(
                R.string.it_support_request_employee,
                item.employeeName,
            )
            metaText.text = itemView.context.getString(
                R.string.it_support_request_meta,
                item.priority,
                item.neededBy.toString(),
            )
            assignButton.setOnClickListener { onAssignAsset(item) }
        }
    }
}

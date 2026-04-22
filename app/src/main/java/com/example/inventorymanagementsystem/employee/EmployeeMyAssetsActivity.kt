package com.example.inventorymanagementsystem.employee

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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class EmployeeMyAssetsActivity : AppCompatActivity() {
    private val adapter = EmployeeAssetsAdapter()

    private lateinit var totalAssetsValue: TextView
    private lateinit var assetHealthValue: TextView
    private lateinit var searchInput: TextInputEditText
    private lateinit var assetsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView

    private var assets: List<EmployeeAsset> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_my_assets)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        setupRecyclerView()
        loadAssets()
        setupSearch()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
    }

    private fun bindViews() {
        totalAssetsValue = findViewById(R.id.totalAssetsValue)
        assetHealthValue = findViewById(R.id.assetHealthValue)
        searchInput = findViewById(R.id.assetSearchInput)
        assetsRecyclerView = findViewById(R.id.assetsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
    }

    private fun setupRecyclerView() {
        assetsRecyclerView.layoutManager = LinearLayoutManager(this)
        assetsRecyclerView.adapter = adapter
    }

    private fun loadAssets() {
        assets = EmployeeAssetRepository.getAssets(EmployeeSessionManager.getSession(this))
        val summary = EmployeeAssetRepository.buildSummary(assets)
        totalAssetsValue.text = summary.totalAssets.toString()
        assetHealthValue.text = EmployeeAssetRepository.buildHealthSummary(assets)
        filterAssets(searchInput.text?.toString().orEmpty())
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAssets(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun filterAssets(query: String) {
        val normalizedQuery = query.trim()
        val filteredAssets = if (normalizedQuery.isBlank()) {
            assets
        } else {
            assets.filter { asset ->
                asset.name.contains(normalizedQuery, ignoreCase = true) ||
                    asset.assetId.contains(normalizedQuery, ignoreCase = true) ||
                    asset.category.contains(normalizedQuery, ignoreCase = true) ||
                    asset.statusLabel.contains(normalizedQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filteredAssets)
        emptyStateText.visibility = if (filteredAssets.isEmpty()) View.VISIBLE else View.GONE
        assetsRecyclerView.visibility = if (filteredAssets.isEmpty()) View.GONE else View.VISIBLE
    }
}

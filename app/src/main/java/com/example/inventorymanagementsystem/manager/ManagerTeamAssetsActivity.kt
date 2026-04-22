package com.example.inventorymanagementsystem.manager

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

class ManagerTeamAssetsActivity : AppCompatActivity() {
    private val adapter = ManagerTeamAssetsAdapter()

    private lateinit var assetSearchInput: TextInputEditText
    private lateinit var assetsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var teamAssetCountText: TextView
    private lateinit var maintenanceCountText: TextView

    private var assets: List<ManagerTeamAsset> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manager_team_assets)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        bindViews()
        assetsRecyclerView.layoutManager = LinearLayoutManager(this)
        assetsRecyclerView.adapter = adapter
        assetSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAssets(s?.toString().orEmpty())
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
        assets = ManagerRepository.getTeamAssets()
        teamAssetCountText.text = assets.size.toString()
        maintenanceCountText.text = assets.count { it.status.equals("Maintenance", ignoreCase = true) }.toString()
        filterAssets(assetSearchInput.text?.toString().orEmpty())
    }

    private fun bindViews() {
        assetSearchInput = findViewById(R.id.assetSearchInput)
        assetsRecyclerView = findViewById(R.id.teamAssetsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        teamAssetCountText = findViewById(R.id.teamAssetCountText)
        maintenanceCountText = findViewById(R.id.maintenanceCountText)
    }

    private fun filterAssets(query: String) {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isBlank()) {
            assets
        } else {
            assets.filter { asset ->
                asset.name.contains(normalizedQuery, ignoreCase = true) ||
                    asset.assetId.contains(normalizedQuery, ignoreCase = true) ||
                    asset.owner.contains(normalizedQuery, ignoreCase = true) ||
                    asset.status.contains(normalizedQuery, ignoreCase = true) ||
                    asset.department.contains(normalizedQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)
        emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        assetsRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}

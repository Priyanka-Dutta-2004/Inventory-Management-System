package com.example.inventorymanagementsystem.employee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class EmployeeAssetsAdapter : RecyclerView.Adapter<EmployeeAssetsAdapter.AssetViewHolder>() {
    private val items = mutableListOf<EmployeeAsset>()

    fun submitList(assets: List<EmployeeAsset>) {
        items.clear()
        items.addAll(assets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.assetCard)
        private val title: TextView = itemView.findViewById(R.id.assetTitle)
        private val meta: TextView = itemView.findViewById(R.id.assetMeta)
        private val details: TextView = itemView.findViewById(R.id.assetDetails)
        private val chipGroup: ChipGroup = itemView.findViewById(R.id.assetChipGroup)

        fun bind(asset: EmployeeAsset) {
            val context = itemView.context
            title.text = asset.name
            meta.text = EmployeeAssetRepository.formatMeta(asset)
            details.text = context.getString(
                R.string.employee_asset_details_format,
                asset.category,
                asset.statusLabel,
                asset.location,
            )

            chipGroup.removeAllViews()
            asset.tags.take(3).forEach { label ->
                val chip = Chip(context).apply {
                    text = label
                    isClickable = false
                    isCheckable = false
                    setEnsureMinTouchTargetSize(false)
                }
                chipGroup.addView(chip)
            }

            val strokeColor = if (asset.state == EmployeeAssetState.ATTENTION) {
                context.getColor(R.color.accent)
            } else {
                context.getColor(R.color.border_soft)
            }
            card.strokeColor = strokeColor
            card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.employee_asset_card_stroke)
        }
    }
}

package com.example.inventorymanagementsystem.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R

class ManagerTeamAssetsAdapter : RecyclerView.Adapter<ManagerTeamAssetsAdapter.AssetViewHolder>() {
    private var items: List<ManagerTeamAsset> = emptyList()

    fun submitList(value: List<ManagerTeamAsset>) {
        items = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manager_team_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.assetTitleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.assetSubtitleText)
        private val metaText: TextView = itemView.findViewById(R.id.assetMetaText)
        private val statusText: TextView = itemView.findViewById(R.id.assetStatusText)

        fun bind(item: ManagerTeamAsset) {
            titleText.text = item.name
            subtitleText.text = itemView.context.getString(
                R.string.manager_team_asset_subtitle_format,
                item.owner,
                item.department,
                item.location,
            )
            metaText.text = itemView.context.getString(
                R.string.manager_team_asset_meta_format,
                item.assetId,
                item.coverageType,
                ManagerRepository.formatRequestDate(item.coverageEndDate),
            )
            statusText.text = item.status
        }
    }
}

package com.example.inventorymanagementsystem.manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.google.android.material.button.MaterialButton

class ManagerRequestsAdapter(
    private val onOpenRequest: (ManagerRequestRecord) -> Unit,
) : RecyclerView.Adapter<ManagerRequestsAdapter.RequestViewHolder>() {

    private var items: List<ManagerRequestRecord> = emptyList()

    fun submitList(value: List<ManagerRequestRecord>) {
        items = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manager_request, parent, false)
        return RequestViewHolder(view, onOpenRequest)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class RequestViewHolder(
        itemView: View,
        private val onOpenRequest: (ManagerRequestRecord) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.requestTitleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.requestSubtitleText)
        private val metaText: TextView = itemView.findViewById(R.id.requestMetaText)
        private val statusText: TextView = itemView.findViewById(R.id.requestStatusText)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.requestOpenButton)

        fun bind(item: ManagerRequestRecord) {
            titleText.text = item.assetName
            subtitleText.text = itemView.context.getString(
                R.string.manager_request_card_subtitle_format,
                item.employeeName,
                item.category,
                item.priority,
            )
            metaText.text = itemView.context.getString(
                R.string.manager_request_card_meta_format,
                ManagerRepository.formatRequestDate(item.createdAt),
                ManagerRepository.formatRequestDate(item.neededBy),
            )
            statusText.text = item.decision.toStatusLabel()
            actionButton.text = if (item.decision == ManagerRequestDecision.PENDING) {
                itemView.context.getString(R.string.manager_request_action_review)
            } else {
                itemView.context.getString(R.string.manager_request_action_view)
            }

            val open = View.OnClickListener { onOpenRequest(item) }
            itemView.setOnClickListener(open)
            actionButton.setOnClickListener(open)
        }
    }
}

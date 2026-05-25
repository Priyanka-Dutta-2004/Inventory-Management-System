package com.example.inventorymanagementsystem.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class AdminRequestsAdapter(
    private val onRequestClick: (AdminRequestRecord) -> Unit,
) : RecyclerView.Adapter<AdminRequestsAdapter.RequestViewHolder>() {
    private val items = mutableListOf<AdminRequestRecord>()
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH)

    fun submitList(requests: List<AdminRequestRecord>) {
        items.clear()
        items.addAll(requests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_request, parent, false)
        return RequestViewHolder(view, onRequestClick)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(items[position], dateFormatter)
    }

    override fun getItemCount(): Int = items.size

    class RequestViewHolder(
        itemView: View,
        private val onRequestClick: (AdminRequestRecord) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvAssetName: TextView = itemView.findViewById(R.id.tvAssetName)
        private val tvEmployeeName: TextView = itemView.findViewById(R.id.tvEmployeeName)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvNeededBy: TextView = itemView.findViewById(R.id.tvNeededBy)

        fun bind(request: AdminRequestRecord, dateFormatter: DateTimeFormatter) {
            tvAssetName.text = request.assetName
            tvEmployeeName.text = request.employeeName
            tvPriority.text = request.priority
            tvStatus.text = request.status
            tvNeededBy.text = "Needed by: ${request.neededBy.format(dateFormatter)}"

            itemView.setOnClickListener { onRequestClick(request) }
        }
    }
}

package com.datn.apptravel.ui.aisuggest.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemSavedItineraryBinding
import com.datn.apptravel.ui.aisuggest.data.model.SavedItinerary
import java.text.SimpleDateFormat
import java.util.*

class SavedItineraryAdapter(
    private val onItemClick: (SavedItinerary) -> Unit,
    private val onDeleteClick: (SavedItinerary) -> Unit
) : ListAdapter<SavedItinerary, SavedItineraryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedItineraryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSavedItineraryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedItinerary) {
            binding.apply {
                tvTitle.text = item.title
                tvDestination.text = "📍 ${item.destination}"
                tvDays.text = "${item.days} ngày"
                tvBudget.text = formatMoney(item.budget)
                tvDate.text = formatDate(item.createdAt)

                root.setOnClickListener { onItemClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }

    private fun formatMoney(amount: Long): String {
        return String.format("%,d VNĐ", amount).replace(",", ".")
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedItinerary>() {
        override fun areItemsTheSame(oldItem: SavedItinerary, newItem: SavedItinerary) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SavedItinerary, newItem: SavedItinerary) =
            oldItem == newItem
    }
}
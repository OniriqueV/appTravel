package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemDateTabBinding
import java.text.SimpleDateFormat
import java.util.Locale

data class DateTab(
    val date: String,
    val dayNumber: Int,
    var isSelected: Boolean = false
)

class DateTabAdapter(
    private var dateTabs: List<DateTab>,
    private val onDateSelected: (Int) -> Unit
) : RecyclerView.Adapter<DateTabAdapter.DateTabViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateTabViewHolder {
        val binding = ItemDateTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateTabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateTabViewHolder, position: Int) {
        holder.bind(dateTabs[position], position)
    }

    override fun getItemCount(): Int = dateTabs.size

    inner class DateTabViewHolder(private val binding: ItemDateTabBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(dateTab: DateTab, position: Int) {
            binding.apply {
                tvDateTab.text = formatDate(dateTab.date)
                tvDateTab.isSelected = position == selectedPosition
                
                tvDateTab.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onDateSelected(position)
                }
            }
        }
        
        private fun formatDate(dateString: String): String {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd-M-yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                return date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                return dateString
            }
        }
    }
    
    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
}

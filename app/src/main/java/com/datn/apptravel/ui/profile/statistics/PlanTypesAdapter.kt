package com.datn.apptravels.ui.profile.statistics

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.R
import com.datn.apptravels.databinding.ItemPlanTypeBinding

class PlanTypesAdapter : ListAdapter<PlanTypeItem, PlanTypesAdapter.PlanTypeViewHolder>(
    PlanTypeDiffCallback()
) {

    inner class PlanTypeViewHolder(
        private val binding: ItemPlanTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanTypeItem) {
            binding.apply {
                // Set type name
                tvPlanType.text = item.type

                // Set count
                tvPlanCount.text = item.count.toString()

                // Set percentage
                tvPercentage.text = String.format("%.1f%%", item.percentage)

                // Set progress bar
                progressBar.progress = item.percentage.toInt()

                // Set icon based on type
                ivIcon.setImageResource(getIconForType(item.type))
            }
        }

        private fun getIconForType(type: String): Int {
            return when (type) {
                "Hoạt động" -> R.drawable.ic_plan
                "Chỗ ở" -> R.drawable.ic_lodgingsss
                "Nhà hàng" -> R.drawable.ic_restaurant
                "Máy bay" -> R.drawable.ic_flight
                "Tàu thuyền" -> R.drawable.ic_boat
                "Thuê xe" -> R.drawable.ic_car
                else -> R.drawable.ic_other
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanTypeViewHolder {
        val binding = ItemPlanTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanTypeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PlanTypeDiffCallback : DiffUtil.ItemCallback<PlanTypeItem>() {
    override fun areItemsTheSame(oldItem: PlanTypeItem, newItem: PlanTypeItem): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: PlanTypeItem, newItem: PlanTypeItem): Boolean {
        return oldItem == newItem
    }
}
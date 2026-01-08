package com.datn.apptravels.ui.profile.statistics

import android.util.Log
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
    private val TAG = "PlanTypesAdapter"

    inner class PlanTypeViewHolder(
        private val binding: ItemPlanTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanTypeItem, position: Int) {
            try {
                Log.d(TAG, "Binding item $position: ${item.type} - ${item.count}")

                binding.apply {
                    // Set type name
                    tvPlanType.text = item.type

                    // Set count
                    tvPlanCount.text = item.count.toString()

                    // Set percentage
                    tvPercentage.text = String.format("%.1f%%", item.percentage)

                    // Set progress bar
                    progressBar.progress = item.percentage.toInt()

                    // Set icon with error handling
                    try {
                        val iconRes = getIconForType(item.type)
                        ivIcon.setImageResource(iconRes)
                        Log.d(TAG, "Icon set successfully for ${item.type}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting icon for ${item.type}: ${e.message}")
                        // Use default icon if specific icon not found
                        ivIcon.setImageResource(R.drawable.ic_globe)
                    }
                }

                Log.d(TAG, "Successfully bound item $position")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding item $position: ${e.message}", e)
            }
        }

        private fun getIconForType(type: String): Int {
            return when (type) {
                "Hoạt động" -> R.drawable.ic_attraction
                "Chỗ ở" -> R.drawable.ic_lodgingsss
                "Chuyến bay" -> R.drawable.ic_flight
                "Nhà hàng" -> R.drawable.ic_restaurant
                "Tour du lịch" -> R.drawable.ic_toursss
                "Tàu thuyền" -> R.drawable.ic_boat
                "Tàu hỏa" -> R.drawable.ic_train
                "Tôn giáo" -> R.drawable.ic_religion
                "Thuê xe" -> R.drawable.ic_car
                "Cắm trại" -> R.drawable.ic_location
                "Rạp chiếu phim" -> R.drawable.ic_theater
                "Mua sắm" -> R.drawable.ic_shopping
                "Khác" -> R.drawable.ic_globe
                else -> R.drawable.ic_globe
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanTypeViewHolder {
        Log.d(TAG, "onCreateViewHolder called")
        val binding = ItemPlanTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanTypeViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder called for position $position")
        holder.bind(getItem(position), position)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d(TAG, "getItemCount: $count")
        return count
    }

    override fun submitList(list: List<PlanTypeItem>?) {
        Log.d(TAG, "submitList called with ${list?.size ?: 0} items")
        list?.forEachIndexed { index, item ->
            Log.d(TAG, "  [$index] ${item.type}: ${item.count}")
        }
        super.submitList(list)
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
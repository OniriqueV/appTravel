package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemScheduleActivityBinding
import com.datn.apptravel.ui.trip.model.ScheduleActivity

class ScheduleActivityAdapter(
    private val activities: List<ScheduleActivity>
) : RecyclerView.Adapter<ScheduleActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemScheduleActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    inner class ActivityViewHolder(private val binding: ItemScheduleActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(activity: ScheduleActivity) {
            binding.apply {
                tvActivityTime.text = activity.time
                tvActivityTitle.text = activity.title
                tvActivityLocation.text = activity.description
                
                // If there's an icon, set it
                activity.iconResId?.let { iconResId ->
                    imgActivityType.setImageResource(iconResId)
                }
            }
        }
    }
}
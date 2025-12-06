package com.datn.apptravel.ui.trip.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemScheduleActivityBinding
import com.datn.apptravel.ui.trip.detail.PlanDetailActivity
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
                
                // Set click listener to navigate to plan detail
                root.setOnClickListener {
                    val context = root.context
                    val intent = Intent(context, PlanDetailActivity::class.java).apply {
                        putExtra(PlanDetailActivity.EXTRA_PLAN_ID, activity.id)
                        putExtra(PlanDetailActivity.EXTRA_TRIP_ID, activity.tripId)
                        putExtra(PlanDetailActivity.EXTRA_PLAN_TITLE, activity.title)
                        putExtra(PlanDetailActivity.EXTRA_PLAN_TYPE, activity.type?.name ?: "")
                        putExtra(PlanDetailActivity.EXTRA_START_TIME, activity.fullStartTime ?: activity.time)
                        putExtra(PlanDetailActivity.EXTRA_LOCATION, activity.location ?: activity.description)
                        putExtra(PlanDetailActivity.EXTRA_EXPENSE, activity.expense ?: 0.0)
                        // Use likesCount and commentsCount from model (default to 0 if not available)
                        putExtra(PlanDetailActivity.EXTRA_LIKES_COUNT, 0)
                        putExtra(PlanDetailActivity.EXTRA_COMMENTS_COUNT, 0)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}
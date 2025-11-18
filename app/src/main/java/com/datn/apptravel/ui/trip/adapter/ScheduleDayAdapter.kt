package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemScheduleDayBinding
import com.datn.apptravel.ui.trip.model.ScheduleDay

class ScheduleDayAdapter(
    private var scheduleDays: List<ScheduleDay>
) : RecyclerView.Adapter<ScheduleDayAdapter.ScheduleDayViewHolder>() {

    private var currentSelectedDay = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleDayViewHolder {
        val binding = ItemScheduleDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleDayViewHolder, position: Int) {
        holder.bind(scheduleDays)
    }

    override fun getItemCount(): Int = if (scheduleDays.isNotEmpty()) 1 else 0

    fun updateScheduleDays(newScheduleDays: List<ScheduleDay>) {
        scheduleDays = newScheduleDays
        currentSelectedDay = 0
        notifyDataSetChanged()
    }

    inner class ScheduleDayViewHolder(private val binding: ItemScheduleDayBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private var dateTabAdapter: DateTabAdapter? = null
        
        fun bind(allScheduleDays: List<ScheduleDay>) {
            binding.apply {
                // Setup date tabs
                val dateTabs = allScheduleDays.map { day ->
                    DateTab(
                        date = day.date,
                        dayNumber = day.dayNumber,
                        isSelected = day.dayNumber - 1 == currentSelectedDay
                    )
                }
                
                dateTabAdapter = DateTabAdapter(dateTabs) { selectedPosition ->
                    currentSelectedDay = selectedPosition
                    updateActivities(allScheduleDays[selectedPosition])
                }
                
                rvDateTabs.apply {
                    adapter = dateTabAdapter
                    layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                }
                
                // Show activities for the first day by default
                if (allScheduleDays.isNotEmpty()) {
                    updateActivities(allScheduleDays[currentSelectedDay])
                }
            }
        }
        
        private fun updateActivities(scheduleDay: ScheduleDay) {
            binding.apply {
                if (scheduleDay.activities.isNotEmpty()) {
                    val activitiesAdapter = ScheduleActivityAdapter(scheduleDay.activities)
                    rvScheduleActivities.adapter = activitiesAdapter
                    rvScheduleActivities.layoutManager = LinearLayoutManager(binding.root.context)
                }
            }
        }
    }
}
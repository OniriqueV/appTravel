package com.datn.apptravel.ui.trip.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.data.model.AISuggestedPlan
import com.datn.apptravel.databinding.ItemAiPlanPreviewBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


class AIPlanPreviewAdapter(
    private val scheduleDays: MutableList<PreviewScheduleDay>,
    private val onPlanDeleted: () -> Unit
) : RecyclerView.Adapter<AIPlanPreviewAdapter.DayViewHolder>() {

    private val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayTitle: MaterialTextView = itemView.findViewById(R.id.tvDayTitle)
        private val tvDayDate: MaterialTextView = itemView.findViewById(R.id.tvDayDate)
        private val rvPlans: RecyclerView = itemView.findViewById(R.id.rvPlans)
        
        fun bind(scheduleDay: PreviewScheduleDay) {
            tvDayTitle.text = "Ngày ${scheduleDay.dayNumber}"
            
            try {
                val date = inputFormat.parse(scheduleDay.date)
                tvDayDate.text = if (date != null) dayFormat.format(date) else scheduleDay.date
            } catch (e: Exception) {
                tvDayDate.text = scheduleDay.date
            }
            
            // Setup plans RecyclerView
            val plansAdapter = PlansInDayAdapter(scheduleDay.plans) { deletedPosition ->
                scheduleDay.plans.removeAt(deletedPosition)
                rvPlans.adapter?.notifyItemRemoved(deletedPosition)
                
                // Notify parent that a plan was deleted
                onPlanDeleted()
            }
            
            rvPlans.apply {
                adapter = plansAdapter
                layoutManager = LinearLayoutManager(itemView.context)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(scheduleDays[position])
    }

    override fun getItemCount(): Int = scheduleDays.size

    /**
     * Inner adapter for plans within a single day
     */
    private class PlansInDayAdapter(
        private val plans: MutableList<AISuggestedPlan>,
        private val onDeleteClick: (position: Int) -> Unit
    ) : RecyclerView.Adapter<PlansInDayAdapter.PlanViewHolder>() {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val numberFormat = NumberFormat.getInstance(Locale("vi", "VN"))

        inner class PlanViewHolder(private val binding: ItemAiPlanPreviewBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(plan: AISuggestedPlan) {
                binding.apply {
                    tvPlanName.text = plan.title

                    // Plan type with emoji
                    tvPlanType.text = when (plan.type.toString()) {
                        "ACTIVITY" -> "🎡 Hoạt động"
                        "RESTAURANT" -> "🍽️ Nhà hàng"
                        "LODGING" -> "🏨 Lưu trú"
                        "FLIGHT" -> "✈️ Chuyến bay"
                        "TRAIN" -> "🚄 Tàu hỏa"
                        "BOAT" -> "⛴️ Thuyền"
                        "CAR_RENTAL" -> "🚗 Thuê xe"
                        "TOUR" -> "🗺️ Tour"
                        "THEATER" -> "🎭 Sân khấu"
                        "SHOPPING" -> "🛍️ Mua sắm"
                        "CAMPING" -> "⛺ Cắm trại"
                        "RELIGION" -> "🙏 Tôn giáo"
                        else -> plan.type.toString()
                    }

                    // Time
                    try {
                        val startDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(plan.startTime)
                        
                        if (startDate != null) {
                            tvPlanTime.text = "🕐 ${timeFormat.format(startDate)}"
                        } else {
                            tvPlanTime.text = ""
                        }
                    } catch (e: Exception) {
                        tvPlanTime.text = ""
                    }

                    // Cost
                    if (plan.expense != null && plan.expense > 0) {
                        tvPlanCost.text = "💰 ${numberFormat.format(plan.expense)} VNĐ"
                    } else {
                        tvPlanCost.text = "💰 Miễn phí"
                    }

                    // Delete button - use adapterPosition to avoid closure issues
                    btnDelete.setOnClickListener {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onDeleteClick(position)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
            val binding = ItemAiPlanPreviewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PlanViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
            holder.bind(plans[position])
        }

        override fun getItemCount(): Int = plans.size
    }
}

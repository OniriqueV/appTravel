package com.datn.apptravel.ui.trip.ai.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.AISuggestedPlan
import com.datn.apptravel.databinding.ItemAiSuggestionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AISuggestionAdapter(
    private var suggestions: List<AISuggestedPlan>,
    private val onItemClick: (AISuggestedPlan) -> Unit,
    private val onSelectClick: (AISuggestedPlan) -> Unit
) : RecyclerView.Adapter<AISuggestionAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAiSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: AISuggestedPlan) {
            // Image
            if (!plan.photoUrl.isNullOrEmpty()) {
                Glide.with(binding.ivPlanImage.context)
                    .load(plan.photoUrl)
                    .placeholder(R.drawable.bg_a)
                    .error(R.drawable.bg_a)
                    .centerCrop()
                    .into(binding.ivPlanImage)
            } else {
                binding.ivPlanImage.setImageResource(R.drawable.bg_a)
            }

            // Plan type badge
            binding.tvPlanType.text = plan.type.toDisplayName()
            binding.tvPlanType.setBackgroundResource(getTypeBackground(plan.type))

            // Title
            binding.tvPlanTitle.text = plan.title

            // Address
            binding.tvPlanAddress.text = plan.address

            // Time
            val time = formatTime(plan.startTime)
            binding.tvPlanTime.text = "🕐 $time"

            // Expense
            if (plan.expense != null && plan.expense > 0) {
                binding.tvPlanExpense.text = "💰 ${formatCurrency(plan.expense)}"
            } else {
                binding.tvPlanExpense.text = "💰 Chưa xác định"
            }

            // Description
            if (!plan.description.isNullOrEmpty()) {
                binding.tvPlanDescription.text = plan.description
            } else {
                binding.tvPlanDescription.text = "AI gợi ý địa điểm này phù hợp với lịch trình của bạn"
            }

            // Selection state
            if (plan.isSelected) {
                binding.checkboxSelect.setImageResource(R.drawable.ic_check_circle)
                binding.cardPlan.strokeWidth = 4
                binding.cardPlan.strokeColor = binding.root.context.getColor(R.color.teal_200)
            } else {
                binding.checkboxSelect.setImageResource(R.drawable.ic_circle_outline)
                binding.cardPlan.strokeWidth = 1
                binding.cardPlan.strokeColor = binding.root.context.getColor(R.color.grey_400)
            }

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(plan)
            }

            binding.checkboxSelect.setOnClickListener {
                onSelectClick(plan)
            }

            binding.cardPlan.setOnClickListener {
                onItemClick(plan)
            }
        }

        private fun formatTime(isoTime: String): String {
            return try {
                val inputFormat = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                )
                val outputFormat = SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                )

                val date = inputFormat.parse(isoTime)
                if (date != null) outputFormat.format(date) else isoTime
            } catch (e: Exception) {
                isoTime
            }
        }
        private fun formatCurrency(amount: Double): String {
            return String.format("%,.0f đ", amount)
        }

        private fun getTypeBackground(type: com.datn.apptravel.data.model.PlanType): Int {
            return when (type) {
                com.datn.apptravel.data.model.PlanType.RESTAURANT -> R.drawable.bg_type_restaurant
                com.datn.apptravel.data.model.PlanType.LODGING -> R.drawable.bg_type_lodging
                com.datn.apptravel.data.model.PlanType.ACTIVITY -> R.drawable.bg_type_activity
                com.datn.apptravel.data.model.PlanType.TOUR -> R.drawable.bg_type_tour
                com.datn.apptravel.data.model.PlanType.SHOPPING -> R.drawable.bg_type_shopping
                else -> R.drawable.bg_type_default
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAiSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateSuggestions(newSuggestions: List<AISuggestedPlan>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }
}
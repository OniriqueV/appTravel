package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ItemPlanMapHorizontalBinding
import com.datn.apptravel.ui.trip.model.PlanLocation

class PlanMapAdapter(
    private val plans: List<PlanLocation>,
    private val onPlanClick: (Int, PlanLocation) -> Unit
) : RecyclerView.Adapter<PlanMapAdapter.PlanViewHolder>() {

    private var highlightedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanMapHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position], position)
    }

    override fun getItemCount(): Int = plans.size

    fun highlightPlan(position: Int) {
        val oldPosition = highlightedPosition
        highlightedPosition = position
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (highlightedPosition != -1) {
            notifyItemChanged(highlightedPosition)
        }
    }

    fun clearHighlight() {
        val oldPosition = highlightedPosition
        highlightedPosition = -1
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
    }

    inner class PlanViewHolder(private val binding: ItemPlanMapHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: PlanLocation, position: Int) {
            binding.apply {
                tvPlanName.text = plan.name
                tvTime.text = plan.time
                tvLocation.text = plan.detail
                ivPlanImage.setImageResource(plan.iconResId)

                // Highlight effect
                val isHighlighted = position == highlightedPosition
                if (isHighlighted) {
                    highlightIndicator.visibility = View.VISIBLE
                    cardPlan.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.highlight_background)
                    )
                    cardPlan.cardElevation = 12f
                } else {
                    highlightIndicator.visibility = View.GONE
                    cardPlan.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.white)
                    )
                    cardPlan.cardElevation = 4f
                }

                cardPlan.setOnClickListener {
                    onPlanClick(position, plan)
                }
            }
        }
    }
}

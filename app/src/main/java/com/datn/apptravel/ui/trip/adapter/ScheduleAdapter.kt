package com.datn.apptravel.ui.trip.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ItemPlanConnectorBinding
import com.datn.apptravel.databinding.ItemPlanMapHorizontalBinding
import com.datn.apptravel.databinding.ItemTripDateBinding
import com.datn.apptravel.ui.trip.model.PlanLocation
import com.datn.apptravel.ui.trip.model.ScheduleItem

class ScheduleAdapter(
    private val items: List<ScheduleItem>,
    private val onPlanClick: (Int, PlanLocation) -> Unit,
    private val onConnectorClick: (Int, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var highlightedPlanPosition = -1
    private var highlightedConnectorPosition = -1

    companion object {
        private const val VIEW_TYPE_DATE = 0
        private const val VIEW_TYPE_PLAN = 1
        private const val VIEW_TYPE_CONNECTOR = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ScheduleItem.DateItem -> VIEW_TYPE_DATE
            is ScheduleItem.PlanItem -> VIEW_TYPE_PLAN
            is ScheduleItem.ConnectorItem -> VIEW_TYPE_CONNECTOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val binding = ItemTripDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateViewHolder(binding)
            }
            VIEW_TYPE_PLAN -> {
                val binding = ItemPlanMapHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PlanViewHolder(binding)
            }
            VIEW_TYPE_CONNECTOR -> {
                val binding = ItemPlanConnectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ConnectorViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ScheduleItem.DateItem -> (holder as DateViewHolder).bind(item)
            is ScheduleItem.PlanItem -> (holder as PlanViewHolder).bind(item, position)
            is ScheduleItem.ConnectorItem -> (holder as ConnectorViewHolder).bind(item, position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun highlightPlan(planPosition: Int) {
        val oldPosition = highlightedPlanPosition
        val oldConnectorPosition = highlightedConnectorPosition
        highlightedPlanPosition = planPosition
        highlightedConnectorPosition = -1
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (oldConnectorPosition != -1) {
            notifyItemChanged(oldConnectorPosition)
        }
        if (highlightedPlanPosition != -1) {
            notifyItemChanged(highlightedPlanPosition)
        }
    }
    
    fun highlightConnector(adapterPosition: Int) {
        val oldPlanPosition = highlightedPlanPosition
        val oldConnectorPosition = highlightedConnectorPosition
        highlightedConnectorPosition = adapterPosition
        highlightedPlanPosition = -1
        
        if (oldPlanPosition != -1) {
            notifyItemChanged(oldPlanPosition)
        }
        if (oldConnectorPosition != -1) {
            notifyItemChanged(oldConnectorPosition)
        }
        if (highlightedConnectorPosition != -1) {
            notifyItemChanged(highlightedConnectorPosition)
        }
    }

    inner class DateViewHolder(private val binding: ItemTripDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleItem.DateItem) {
            binding.tvLabel.text = item.label
            binding.tvDate.text = item.date
        }
    }

    inner class PlanViewHolder(private val binding: ItemPlanMapHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleItem.PlanItem, adapterPosition: Int) {
            val plan = item.plan
            val planPos = item.position
            
            binding.apply {
                tvPlanName.text = plan.name
                tvTime.text = plan.time
                tvLocation.text = plan.detail
                ivPlanImage.setImageResource(plan.iconResId)

                // Highlight effect based on plan position, not adapter position
                val isHighlighted = planPos == highlightedPlanPosition
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
                    onPlanClick(planPos, plan)
                }
            }
        }
    }
    
    inner class ConnectorViewHolder(private val binding: ItemPlanConnectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleItem.ConnectorItem, adapterPosition: Int) {
            val isHighlighted = adapterPosition == highlightedConnectorPosition
            
            if (isHighlighted) {
                binding.connectorLine.setBackgroundColor(Color.parseColor("#2563EB"))
            } else {
                binding.connectorLine.setBackgroundColor(Color.parseColor("#E5E7EB"))
            }
            
            binding.root.setOnClickListener {
                onConnectorClick(item.fromPlanPosition, item.toPlanPosition)
            }
        }
    }
}

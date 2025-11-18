package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.data.model.TopicSelection
import com.datn.apptravel.databinding.ItemTopicBinding

class TopicAdapter(
    private val topics: List<TopicSelection>,
    private val onTopicChecked: (TopicSelection, Boolean) -> Unit
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    inner class TopicViewHolder(private val binding: ItemTopicBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(topicSelection: TopicSelection) {
            binding.apply {
                tvTopicName.text = topicSelection.topic.topicName
                ivTopicIcon.setImageResource(topicSelection.topic.iconRes)
                
                // Set selected state for border
                topicContainer.isSelected = topicSelection.isSelected
                
                // Handle container click
                topicContainer.setOnClickListener {
                    topicSelection.isSelected = !topicSelection.isSelected
                    topicContainer.isSelected = topicSelection.isSelected
                    onTopicChecked(topicSelection, topicSelection.isSelected)
                }
            }
        }
    }
}

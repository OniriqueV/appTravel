package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.User
import com.datn.apptravel.databinding.ItemFollowerSelectionBinding
import com.datn.apptravel.utils.ApiConfig

class FollowerSelectionAdapter(
    private val onAddMember: (User) -> Unit,
    private val selectedMembers: List<User>
) : ListAdapter<User, FollowerSelectionAdapter.FollowerViewHolder>(FollowerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
        val binding = ItemFollowerSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FollowerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FollowerViewHolder(
        private val binding: ItemFollowerSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            // Load avatar
            if (!user.profilePicture.isNullOrEmpty()) {
                val imageUrl = when {
                    user.profilePicture.startsWith("http") -> user.profilePicture
                    user.profilePicture.startsWith("data:") -> user.profilePicture
                    else -> "${ApiConfig.TRIP_SERVICE_BASE_URL}${user.profilePicture}"
                }
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivFollowerAvatar)
            } else {
                binding.ivFollowerAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }

            // Set name
            val fullName = "${user.firstName} ${user.lastName}".trim()
            binding.tvFollowerName.text = fullName.ifEmpty { "Unknown User" }

            // Set email
            binding.tvFollowerEmail.text = user.email

            // Check if already added
            val isAdded = selectedMembers.any { it.id == user.id }

            if (isAdded) {
                binding.btnAdd.visibility = View.GONE
                binding.tvAdded.visibility = View.VISIBLE
            } else {
                binding.btnAdd.visibility = View.VISIBLE
                binding.tvAdded.visibility = View.GONE
            }

            // Add button click
            binding.btnAdd.setOnClickListener {
                onAddMember(user)
            }
        }
    }

    private class FollowerDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}

package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.User
import com.datn.apptravel.databinding.ItemTripMemberBinding
import com.datn.apptravel.utils.ApiConfig

class TripMemberAdapter(
    private val onRemoveMember: (User) -> Unit
) : ListAdapter<User, TripMemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemTripMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemTripMemberBinding
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
                    .into(binding.ivMemberAvatar)
            } else {
                binding.ivMemberAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }

            // Set member name
            val displayName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
            binding.tvMemberName.text = if (displayName.isNotEmpty()) displayName else "Unknown"

            // Remove member button - use current item from adapter position
            binding.ivRemoveMember.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val currentUser = getItem(position)
                    android.util.Log.d("TripMemberAdapter", "Remove clicked for: ${currentUser.firstName} at position $position")
                    onRemoveMember(currentUser)
                }
            }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}

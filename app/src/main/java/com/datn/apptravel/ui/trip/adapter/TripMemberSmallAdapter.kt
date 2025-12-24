package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.User
import com.datn.apptravel.databinding.ItemTripMemberSmallBinding
import com.datn.apptravel.utils.ApiConfig

class TripMemberSmallAdapter : ListAdapter<User, TripMemberSmallAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemTripMemberSmallBinding.inflate(
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
        private val binding: ItemTripMemberSmallBinding
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

package com.datn.apptravels.ui.profile.badges

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.R
import com.datn.apptravels.data.model.Badge
import com.datn.apptravels.databinding.ItemBadgeBinding

class BadgeAdapter(
    private val isEarned: Boolean
) : ListAdapter<Badge, BadgeAdapter.BadgeViewHolder>(BadgeDiffCallback()) {

    inner class BadgeViewHolder(
        private val binding: ItemBadgeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: Badge) {
            binding.apply {
                // Set badge name
                tvBadgeName.text = badge.name

                // Set badge description
                tvBadgeDescription.text = badge.description

                // Set badge icon
                ivBadgeIcon.setImageResource(getBadgeIcon(badge.id))

                if (isEarned) {
                    // Earned badge - full color
                    ivBadgeIcon.colorFilter = null
                    ivBadgeIcon.alpha = 1f
                    tvBadgeName.alpha = 1f
                    tvBadgeDescription.alpha = 1f
                    viewLock.visibility = View.GONE
                } else {
                    // Locked badge - grayscale and semi-transparent
                    applyGrayscale()
                    ivBadgeIcon.alpha = 0.5f
                    tvBadgeName.alpha = 0.5f
                    tvBadgeDescription.alpha = 0.5f
                    viewLock.visibility = View.VISIBLE
                }
            }
        }

        private fun applyGrayscale() {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            binding.ivBadgeIcon.colorFilter = filter
        }

        private fun getBadgeIcon(badgeId: String): Int {
            return when (badgeId) {
                "first_trip" -> R.drawable.badge_first_trip
                "trip_master_5" -> R.drawable.badge_trip_master
                "globe_trotter_10" -> R.drawable.badge_globe_trotter
                "organized_planner_10" -> R.drawable.badge_organized_planner
                "master_planner_50" -> R.drawable.badge_master_planner
                "document_keeper_5" -> R.drawable.badge_document_keeper
                "archive_master_20" -> R.drawable.badge_archive_master
                else -> R.drawable.ic_badge
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class BadgeDiffCallback : DiffUtil.ItemCallback<Badge>() {
    override fun areItemsTheSame(oldItem: Badge, newItem: Badge): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Badge, newItem: Badge): Boolean {
        return oldItem == newItem
    }
}
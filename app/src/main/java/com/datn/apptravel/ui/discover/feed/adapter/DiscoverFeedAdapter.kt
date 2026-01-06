package com.datn.apptravel.ui.discover.feed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.network.FollowRepository
import com.datn.apptravel.ui.discover.util.ImageUrlUtil
import com.datn.apptravel.ui.discover.util.TimeUtil
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DiscoverFeedAdapter(
    private val currentUserId: String?,
    private val items: MutableList<DiscoverItem>,
    private val followRepository: FollowRepository,
    private val lifecycleOwner: LifecycleOwner,
    private val onTripClick: (String) -> Unit,
    private val onUserClick: (String) -> Unit,
    private val onFollowChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<DiscoverFeedAdapter.VH>() {

    /* =========================
       PUBLIC API FOR FRAGMENT
       ========================= */

    fun submitList(list: List<DiscoverItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    /** 🔥 HÀM QUAN TRỌNG – FEED UPDATE REALTIME */
    fun updateTripLikeCount(tripId: String, delta: Int) {
        val index = items.indexOfFirst { it.tripId == tripId }
        if (index == -1) return

        val item = items[index]
        items[index] = item.copy(
            likeCount = (item.likeCount + delta).coerceAtLeast(0)
        )
        notifyItemChanged(index)
    }

    /* =========================
       ADAPTER
       ========================= */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_post, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    /* =========================
       VIEW HOLDER
       ========================= */

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        private val btnFollow: MaterialButton = itemView.findViewById(R.id.btnFollow)

        fun bind(item: DiscoverItem) {

            tvUserName.text = item.userName ?: "Unknown"
            tvTime.text = TimeUtil.formatTimeAgo(item.sharedAt ?: "")

            if (item.caption.isNullOrBlank()) {
                tvCaption.visibility = View.GONE
            } else {
                tvCaption.visibility = View.VISIBLE
                tvCaption.text = item.caption
            }

            // Avatar
            if (item.userAvatar.isNullOrBlank()) {
                imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            } else {
                Glide.with(imgAvatar)
                    .load(item.userAvatar)
                    .transform(CircleCrop())
                    .into(imgAvatar)
            }

            imgAvatar.setOnClickListener {
                item.userId?.let(onUserClick)
            }
            tvUserName.setOnClickListener {
                item.userId?.let(onUserClick)
            }

            // Trip image
            Glide.with(imgPost)
                .load(ImageUrlUtil.toFullUrl(item.tripImage))
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imgPost)

            imgPost.setOnClickListener {
                item.tripId?.let(onTripClick)
            }

            // Follow
            if (item.userId == currentUserId || item.isFollowing) {
                btnFollow.visibility = View.GONE
            } else {
                btnFollow.visibility = View.VISIBLE
                btnFollow.setOnClickListener {
                    follow(item)
                }
            }
        }

        private fun follow(item: DiscoverItem) {
            val me = currentUserId ?: return
            val target = item.userId ?: return

            items.forEachIndexed { index, it ->
                if (it.userId == target) {
                    it.isFollowing = true
                    notifyItemChanged(index)
                }
            }

            lifecycleOwner.lifecycleScope.launch {
                try {
                    followRepository.follow(me, target)
                    onFollowChanged(target, true)
                } catch (e: Exception) {
                    items.forEachIndexed { index, it ->
                        if (it.userId == target) {
                            it.isFollowing = false
                            notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }
}

package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.post.ImageUrlUtil

class DiscoverFeedAdapter(
    private val onPostClick: (DiscoverItem) -> Unit,
    private val onComment: (String) -> Unit
) : RecyclerView.Adapter<DiscoverFeedAdapter.VH>() {

    private val items = mutableListOf<DiscoverItem>()

    fun submitList(list: List<DiscoverItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_post, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val btnComment: TextView = itemView.findViewById(R.id.btnComment)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)

        fun bind(item: DiscoverItem) {

            tvUserName.text = item.userName ?: "Người dùng"
            tvCaption.text = item.caption ?: ""
            tvTime.text = formatTimeAgo(item.createdAt ?: 0L)

            Glide.with(itemView)
                .load(ImageUrlUtil.toFullUrl(item.userAvatar))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .transform(CircleCrop())
                .into(imgAvatar)

            Glide.with(itemView)
                .load(ImageUrlUtil.toFullUrl(item.tripImage))
                .placeholder(R.drawable.bg_trip_placeholder)
                .error(R.drawable.bg_trip_placeholder)
                .into(imgPost)

            tvLikeCount.text = (item.likesCount ?: 0L).toString()
            btnComment.text = "💬 ${item.commentsCount ?: 0}"

            // ❗ FEED CHỈ HIỂN THỊ – KHÔNG TOGGLE LIKE
            btnLike.setImageResource(R.drawable.ic_heart_outline)
            btnLike.setColorFilter(0xFF666666.toInt())

            // 👉 Click mở PostDetail
            itemView.setOnClickListener { onPostClick(item) }
            imgPost.setOnClickListener { onPostClick(item) }

            // ❤️ Click tim → mở detail (UX giống Facebook)
            btnLike.setOnClickListener {
                btnLike.animate()
                    .scaleX(1.2f).scaleY(1.2f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(120)
                    .withEndAction {
                        btnLike.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }
                    .start()

                onPostClick(item)
            }

            // 💬 Comment
            btnComment.setOnClickListener {
                item.postId?.let { onComment(it) }
            }
        }
    }

    private fun formatTimeAgo(createdAtMillis: Long): String {
        if (createdAtMillis <= 0L) return ""
        val diff = System.currentTimeMillis() - createdAtMillis
        val min = diff / 60_000
        val hour = diff / 3_600_000
        val day = diff / 86_400_000

        return when {
            min < 1 -> "Vừa xong"
            min < 60 -> "$min phút trước"
            hour < 24 -> "$hour giờ trước"
            day < 7 -> "$day ngày trước"
            else -> "${day / 7} tuần trước"
        }
    }
}

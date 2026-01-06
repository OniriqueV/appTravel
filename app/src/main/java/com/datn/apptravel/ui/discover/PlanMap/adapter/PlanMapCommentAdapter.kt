package com.datn.apptravel.ui.discover.PlanMap.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.PlanCommentDto
import com.datn.apptravel.ui.discover.util.ImageUrlUtil
import com.datn.apptravel.ui.discover.util.TimeUtil

class PlanMapCommentAdapter(
    private val currentUserId: String,
    private val onLongClick: (PlanCommentDto) -> Unit,
    private val onReplyClick: (PlanCommentDto) -> Unit
) : RecyclerView.Adapter<PlanMapCommentAdapter.VH>() {

    private var isOwner: Boolean = false
    private val items = mutableListOf<PlanCommentDto>()

    fun setOwner(value: Boolean) {
        if (isOwner != value) {
            isOwner = value
            notifyDataSetChanged()
        }
    }

    fun submit(list: List<PlanCommentDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_map_comment, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvUser.text = item.userName ?: "Unknown"
        holder.tvContent.text = item.content.trim().replace("\"", "")

        val isReply = item.parentId != null

        // 🔥 Time
        if (!item.createdAt.isNullOrBlank()) {
            holder.tvTime.visibility = View.VISIBLE
            holder.tvTime.text = TimeUtil.formatTimeAgo(item.createdAt)
        } else {
            holder.tvTime.visibility = View.GONE
        }

        // 🔥 Reply button
        holder.tvReply.visibility =
            if (isReply) View.GONE else View.VISIBLE

        if (!isReply) {
            holder.tvReply.setOnClickListener {
                onReplyClick(item)
            }
        }

        // 🔥 Avatar
        holder.imgAvatar.visibility =
            if (isReply) View.INVISIBLE else View.VISIBLE

        if (!isReply) {
            val avatarUrl = item.userAvatar?.trim()
            if (!avatarUrl.isNullOrEmpty()) {
                holder.imgAvatar.load(ImageUrlUtil.toFullUrl(avatarUrl)) {
                    placeholder(R.drawable.ic_avatar_placeholder)
                    error(R.drawable.ic_avatar_placeholder)
                    crossfade(true)
                }
            } else {
                holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        }

        // 🔥 Padding
        val startPadding = if (isReply) dp(holder.itemView, 48) else dp(holder.itemView, 8)
        holder.itemView.setPadding(
            startPadding,
            dp(holder.itemView, 6),
            dp(holder.itemView, 8),
            dp(holder.itemView, 6)
        )

        // 🔥 Delete
        val canDelete = isOwner || item.userId == currentUserId
        holder.itemView.setOnLongClickListener {
            if (canDelete) onLongClick(item)
            true
        }

        holder.itemView.foreground =
            if (canDelete) getSelectableItemBackground(holder.itemView)
            else null
    }



    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val tvReply: TextView = view.findViewById(R.id.tvReply)
    }

    private fun getSelectableItemBackground(view: View) =
        TypedValue().let { outValue ->
            view.context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
            )
            view.context.getDrawable(outValue.resourceId)
        }

    private fun dp(view: View, value: Int): Int =
        (value * view.resources.displayMetrics.density).toInt()
}

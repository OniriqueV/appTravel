package com.datn.apptravels.ui.discover.PlanMap.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.model.PlanCommentDto
import com.datn.apptravels.ui.discover.util.ImageUrlUtil
import com.datn.apptravels.ui.discover.util.TimeUtil

class PlanMapCommentAdapter(
    private val currentUserId: String,
    private val onLongClick: (PlanCommentDto) -> Unit,
    private val onReplyClick: (PlanCommentDto, PlanCommentDto) -> Unit
) : RecyclerView.Adapter<PlanMapCommentAdapter.VH>() {

    companion object {
        private const val TYPE_PARENT = 0
        private const val TYPE_REPLY = 1
    }

    private var isOwner: Boolean = false

    private val allComments = mutableListOf<PlanCommentDto>()
    private val items = mutableListOf<PlanCommentDto>()
    private val expandedParents = mutableSetOf<String>()

    fun setOwner(value: Boolean) {
        if (isOwner != value) {
            isOwner = value
            notifyDataSetChanged()
        }
    }

    fun submit(list: List<PlanCommentDto>) {
        allComments.clear()
        allComments.addAll(list)
        rebuildDisplayList()
    }

    private fun rebuildDisplayList() {
        items.clear()

        val grouped = allComments.groupBy { it.parentId }
        val parents = grouped[null].orEmpty()

        for (parent in parents) {
            items.add(parent)

            val key = parent.id.toString()
            if (expandedParents.contains(key)) {
                items.addAll(grouped[key].orEmpty())
            }
        }

        notifyDataSetChanged()
    }

    private fun repliesCountFor(parentId: Long): Int {
        val key = parentId.toString()
        return allComments.count { it.parentId == key }
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].parentId == null) TYPE_PARENT else TYPE_REPLY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout =
            if (viewType == TYPE_PARENT)
                R.layout.item_comment_plan
            else
                R.layout.item_comment_plan_reply

        return VH(
            LayoutInflater.from(parent.context)
                .inflate(layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val isReply = getItemViewType(position) == TYPE_REPLY

        holder.tvUser.text = item.userName ?: "Unknown"
        holder.tvContent.text = item.content.trim().replace("\"", "")

        if (!item.createdAt.isNullOrBlank()) {
            holder.tvTime.visibility = View.VISIBLE
            holder.tvTime.text = TimeUtil.formatTimeAgo(item.createdAt)
        } else {
            holder.tvTime.visibility = View.GONE
        }

        // ✅ Reply (cha + con đều reply được, nhưng LUÔN về cha)
        holder.tvReply?.visibility = View.VISIBLE
        holder.tvReply?.setOnClickListener {
            val parent = if (item.parentId == null) {
                item
            } else {
                allComments.first { it.id.toString() == item.parentId }
            }
            onReplyClick(item, parent)
        }

        // View / Hide replies (chỉ comment cha)
        holder.btnViewReplies?.let { btn ->
            val count = repliesCountFor(item.id)
            val key = item.id.toString()
            val expanded = expandedParents.contains(key)

            btn.visibility =
                if (!isReply && count > 0) View.VISIBLE else View.GONE

            btn.text =
                if (expanded) "Hide $count replies"
                else "View $count replies"

            btn.setOnClickListener {
                if (expanded) expandedParents.remove(key)
                else expandedParents.add(key)
                rebuildDisplayList()
            }
        }

        // Avatar
        holder.imgAvatar?.let { avatar ->
            avatar.setImageDrawable(null)
            avatar.visibility = View.VISIBLE

            val url = item.userAvatar?.trim()
            if (!url.isNullOrEmpty()) {
                avatar.load(ImageUrlUtil.toFullUrl(url)) {
                    placeholder(R.drawable.ic_avatar_placeholder)
                    error(R.drawable.ic_avatar_placeholder)
                }
            } else {
                avatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        }

        /* =====================================================
           🔥 FIX CHÍNH: LONG CLICK GẮN VÀO commentBubble
           ===================================================== */
        val canDelete = isOwner || item.userId == currentUserId

        holder.commentBubble.setOnLongClickListener {
            if (canDelete) {
                onLongClick(item)
            }
            true
        }

        holder.commentBubble.foreground =
            if (canDelete) getSelectableItemBackground(holder.commentBubble)
            else null
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUserName)
        val tvContent: TextView = view.findViewById(R.id.tvCommentContent)
        val tvTime: TextView = view.findViewById(R.id.tvTimeAgo)

        val imgAvatar: ImageView? = view.findViewById(R.id.ivUserAvatar)
        val tvReply: TextView? = view.findViewById(R.id.btnReply)
        val btnViewReplies: TextView? = view.findViewById(R.id.btnViewReplies)

        // 🔥 QUAN TRỌNG
        val commentBubble: View = view.findViewById(R.id.commentBubble)
    }

    private fun getSelectableItemBackground(view: View) =
        TypedValue().let {
            view.context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                it,
                true
            )
            view.context.getDrawable(it.resourceId)
        }

    fun expandParent(parentId: Long) {
        val key = parentId.toString()
        if (!expandedParents.contains(key)) {
            expandedParents.add(key)
            rebuildDisplayList()
        }
    }
}

package com.datn.apptravels.ui.trip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.model.CommentDto
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val onReplyClick: (CommentDto) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val VIEW_TYPE_PARENT = 0
        private const val VIEW_TYPE_REPLY = 1
    }
    
    private val displayItems = mutableListOf<CommentItem>()
    private val allComments = mutableListOf<CommentDto>()
    
    fun updateComments(newComments: List<CommentDto>) {
        allComments.clear()
        allComments.addAll(newComments)
        
        // Build hierarchy: separate parents and replies
        val parentComments = newComments.filter { it.parentId.isNullOrEmpty() }
        val allReplies = newComments.filter { !it.parentId.isNullOrEmpty() }
        
        // Build display items with only parent comments initially
        displayItems.clear()
        parentComments.forEach { parent ->
            // Find only direct replies (not nested)
            val directReplies = allReplies.filter { it.parentId == parent.id.toString() }
            displayItems.add(CommentItem(
                comment = parent,
                isReply = false,
                level = 0,
                replies = directReplies,
                isRepliesVisible = false
            ))
        }
        
        notifyDataSetChanged()
    }
    
    private fun buildReplyItemsWithLevel(replies: List<CommentDto>, parentLevel: Int): List<CommentItem> {
        val result = mutableListOf<CommentItem>()
        val allReplies = allComments.filter { !it.parentId.isNullOrEmpty() }
        
        replies.forEach { reply ->
            // Find direct children of this reply
            val directChildren = allReplies.filter { it.parentId == reply.id.toString() }
            
            result.add(CommentItem(
                comment = reply,
                isReply = true,
                level = parentLevel + 1,
                replies = directChildren,
                isRepliesVisible = false
            ))
        }
        
        return result
    }
    
    private fun countNestedItems(position: Int): Int {
        val parentLevel = displayItems[position].level
        var count = 0
        var currentPos = position + 1
        
        while (currentPos < displayItems.size && displayItems[currentPos].level > parentLevel) {
            count++
            currentPos++
        }
        
        return count
    }
    
    private fun toggleReplies(position: Int) {
        val item = displayItems[position]
        if (item.replies.isEmpty()) return
        
        item.isRepliesVisible = !item.isRepliesVisible
        
        if (item.isRepliesVisible) {
            // Insert direct replies after parent
            val replyItems = buildReplyItemsWithLevel(item.replies, item.level)
            displayItems.addAll(position + 1, replyItems)
            notifyItemChanged(position) // Update parent button text
            notifyItemRangeInserted(position + 1, replyItems.size)
        } else {
            // Remove all nested replies (recursively)
            val removeCount = countNestedItems(position)
            for (i in 0 until removeCount) {
                displayItems.removeAt(position + 1)
            }
            notifyItemChanged(position) // Update parent button text
            notifyItemRangeRemoved(position + 1, removeCount)
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (displayItems[position].isReply) {
            VIEW_TYPE_REPLY
        } else {
            VIEW_TYPE_PARENT
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_PARENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment_plan, parent, false)
            ParentCommentViewHolder(view, onReplyClick, ::toggleReplies)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment_reply, parent, false)
            ReplyCommentViewHolder(view, onReplyClick, ::toggleReplies)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]
        when (holder) {
            is ParentCommentViewHolder -> holder.bind(item, position)
            is ReplyCommentViewHolder -> holder.bind(item)
        }
    }
    
    override fun getItemCount(): Int = displayItems.size
    
    class ParentCommentViewHolder(
        itemView: View,
        private val onReplyClick: (CommentDto) -> Unit,
        private val onToggleReplies: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvCommentContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReply)
        private val btnViewReplies: TextView = itemView.findViewById(R.id.btnViewReplies)
        
        fun bind(item: CommentItem, position: Int) {
            val comment = item.comment
            
            // Load user avatar
            if (!comment.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.userAvatar)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivUserAvatar)
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_profile)
            }
            
            // Set user name and content
            tvUserName.text = comment.userName
            tvCommentContent.text = comment.content
            
            // Set time ago
            tvTimeAgo.text = getTimeAgo(comment.createdAt)
            
            // Set reply click listener
            btnReply.setOnClickListener {
                onReplyClick(comment)
            }
            
            // Show/hide view replies button
            if (item.replies.isNotEmpty()) {
                btnViewReplies.visibility = View.VISIBLE
                val replyCount = item.replies.size
                btnViewReplies.text = if (item.isRepliesVisible) {
                    "Hide $replyCount ${if (replyCount == 1) "reply" else "replies"}"
                } else {
                    "View $replyCount ${if (replyCount == 1) "reply" else "replies"}"
                }
                btnViewReplies.setOnClickListener {
                    onToggleReplies(position)
                }
            } else {
                btnViewReplies.visibility = View.GONE
            }
        }
        
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
                diff < 2592000_000 -> "${diff / 604800_000}w"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
    
    class ReplyCommentViewHolder(
        itemView: View,
        private val onReplyClick: (CommentDto) -> Unit,
        private val onToggleReplies: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvCommentContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReply)
        private val btnViewReplies: TextView = itemView.findViewById(R.id.btnViewReplies)
        
        fun bind(item: CommentItem) {
            val comment = item.comment
            
            // Apply dynamic indent based on level
            val baseIndent = 44 // Base indent in dp
            val indentPerLevel = 44 // Additional indent per level in dp
            val totalIndent = baseIndent + (item.level - 1) * indentPerLevel
            
            val density = itemView.context.resources.displayMetrics.density
            val indentPx = (totalIndent * density).toInt()
            
            // Update avatar margin
            val avatarParams = ivUserAvatar.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            avatarParams.marginStart = indentPx
            ivUserAvatar.layoutParams = avatarParams
            
            // Update line margin
            val lineView = itemView.findViewById<View>(R.id.replyLine)
            val lineParams = lineView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            lineParams.marginStart = indentPx - (4 * density).toInt()
            lineView.layoutParams = lineParams
            
            // Load user avatar
            if (!comment.userAvatar.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.userAvatar)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivUserAvatar)
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_profile)
            }
            
            // Set user name and content
            tvUserName.text = comment.userName
            tvCommentContent.text = comment.content
            
            // Set time ago
            tvTimeAgo.text = getTimeAgo(comment.createdAt)
            
            // Set reply click listener
            btnReply.setOnClickListener {
                onReplyClick(comment)
            }
            
            // Show/hide view replies button
            if (item.replies.isNotEmpty()) {
                btnViewReplies.visibility = View.VISIBLE
                val replyCount = item.replies.size
                btnViewReplies.text = if (item.isRepliesVisible) {
                    "Hide $replyCount ${if (replyCount == 1) "reply" else "replies"}"
                } else {
                    "View $replyCount ${if (replyCount == 1) "reply" else "replies"}"
                }
                btnViewReplies.setOnClickListener {
                    onToggleReplies(bindingAdapterPosition)
                }
            } else {
                btnViewReplies.visibility = View.GONE
            }
        }
        
        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
                diff < 2592000_000 -> "${diff / 604800_000}w"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}

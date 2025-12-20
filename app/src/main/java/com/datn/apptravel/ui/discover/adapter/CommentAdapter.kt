package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.CommentUiModel
import com.datn.apptravel.ui.discover.util.TimeUtil



class CommentAdapter :
    ListAdapter<CommentUiModel, CommentAdapter.CommentVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentVH(v)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(item: CommentUiModel) {
            tvName.text = item.userName
            tvContent.text = item.content
            tvTime.text = TimeUtil.formatTimeAgo(item.createdAt)

            Glide.with(itemView)
                .load(item.userAvatar)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(imgAvatar)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CommentUiModel>() {
            override fun areItemsTheSame(o: CommentUiModel, n: CommentUiModel) = o.id == n.id
            override fun areContentsTheSame(o: CommentUiModel, n: CommentUiModel) = o == n
        }
    }
}
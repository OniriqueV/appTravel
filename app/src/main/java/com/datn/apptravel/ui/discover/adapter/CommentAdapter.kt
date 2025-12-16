package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.PostComment

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.VH>() {

    private val list = mutableListOf<PostComment>()

    fun submit(data: List<PostComment>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return VH(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(list[position])
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {

        private val imgAvatar: ImageView = v.findViewById(R.id.imgAvatar)
        private val tvUserName: TextView = v.findViewById(R.id.tvUserName)
        private val tvContent: TextView = v.findViewById(R.id.tvContent)

        fun bind(c: PostComment) {
            tvUserName.text = c.userName ?: "User"
            tvContent.text = c.content

            Glide.with(itemView.context)
                .load(c.avatar)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(imgAvatar)
        }
    }
}

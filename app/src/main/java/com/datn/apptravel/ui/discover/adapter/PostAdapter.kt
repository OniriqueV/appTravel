package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.Post

class PostAdapter(
    private var items: List<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onCommentClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val imgPost: ImageView = itemView.findViewById(R.id.imgPost)
        val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)

        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val tvLikes: TextView = itemView.findViewById(R.id.tvLikes)
        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val item = items[position]

        holder.tvUserName.text = item.userName
        holder.tvCaption.text = item.caption
        holder.tvLocation.text = item.location ?: ""
        holder.imgAvatar.setImageResource(item.userAvatarRes)
        holder.imgPost.setImageResource(item.imageRes)

        // hiển thị số like
        holder.tvLikes.text = "${item.likes} likes"

        // hiển thị trạng thái like
        holder.btnLike.setImageResource(
            if (item.isLiked) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        // xử lý click like
        holder.btnLike.setOnClickListener {
            onLikeClicked(item)
        }

        // xử lý click comment
        holder.btnComment.setOnClickListener {
            onCommentClicked(item)
        }
    }

    fun submitList(newItems: List<Post>) {
        items = newItems
        notifyDataSetChanged()
    }
}

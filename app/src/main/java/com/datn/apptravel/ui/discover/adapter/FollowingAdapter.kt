package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.FollowUser

class FollowingAdapter(
    private var items: List<FollowUser>
) : RecyclerView.Adapter<FollowingAdapter.FollowViewHolder>() {

    inner class FollowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatarFollow)
        val tvName: TextView = itemView.findViewById(R.id.tvNameFollow)
        val tvBio: TextView = itemView.findViewById(R.id.tvBioFollow)
        val btnUnfollow: Button = itemView.findViewById(R.id.btnUnfollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return FollowViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvBio.text = item.bio ?: ""
        holder.imgAvatar.setImageResource(item.avatarRes)

        // TODO: thêm logic unfollow nếu cần
        holder.btnUnfollow.setOnClickListener {
            // handle click
        }
    }

    fun submitList(newItems: List<FollowUser>) {
        items = newItems
        notifyDataSetChanged()
    }
}

package com.datn.apptravels.ui.discover.profileFollow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.ui.discover.util.ImageUrlUtil
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.profileFollow.model.FollowerDto


class FollowersAdapter(
    private val items: MutableList<FollowerDto>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<FollowersAdapter.VH>() {

    fun submitList(list: List<FollowerDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follower, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvName)

        fun bind(user: FollowerDto) {
            tvName.text = user.userName

            Glide.with(itemView)
                .load(ImageUrlUtil.toFullUrl(user.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(imgAvatar)

            itemView.setOnClickListener {
                onUserClick(user.userId)
            }
        }
    }
}



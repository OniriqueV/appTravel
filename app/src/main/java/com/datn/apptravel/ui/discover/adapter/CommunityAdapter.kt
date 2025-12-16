package com.datn.apptravel.ui.discover.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.CommunityItem

class CommunityAdapter(
    private val items: List<CommunityItem>
) : RecyclerView.Adapter<CommunityAdapter.CommunityViewHolder>() {

    inner class CommunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCommunity)
        val tvTitle: TextView = itemView.findViewById(R.id.tvCommunityTitle)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgUserAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community, parent, false)
        return CommunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        val item = items[position]

        holder.imgCover.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvUserName.text = item.userName
        holder.imgAvatar.setImageResource(item.userAvatarRes)
    }

    override fun getItemCount(): Int = items.size
}

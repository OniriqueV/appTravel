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
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.DiscoverItem

class DiscoverFeedAdapter(
    private val onPostClick: (DiscoverItem) -> Unit
) : RecyclerView.Adapter<DiscoverFeedAdapter.VH>() {

    private val items = mutableListOf<DiscoverItem>()

    fun submitList(data: List<DiscoverItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_post, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: DiscoverItem) {

            itemView.findViewById<TextView>(R.id.tvTitle).text = item.title
            itemView.findViewById<TextView>(R.id.tvUserName).text = item.userName

            Glide.with(itemView)
                .load(item.coverPhoto)
                .into(itemView.findViewById(R.id.imgPostCover))

            // ✅ CLICK → POST DETAIL
            itemView.setOnClickListener {
                onPostClick(item)
            }
        }
    }
}


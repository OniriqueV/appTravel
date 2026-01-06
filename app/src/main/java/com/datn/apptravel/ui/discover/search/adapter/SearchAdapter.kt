package com.datn.apptravel.ui.discover.search.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.search.SearchItem

class SearchAdapter(
    private val onUserClick: (String) -> Unit,
    private val onTripClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<SearchItem>()

    fun submit(list: List<SearchItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SearchItem.Header -> 0
            is SearchItem.UserItem -> 1
            is SearchItem.TripItem -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderVH(inflater.inflate(R.layout.item_search_header, parent, false))
            1 -> UserVH(inflater.inflate(R.layout.item_search_user, parent, false))
            else -> TripVH(inflater.inflate(R.layout.item_search_trip, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchItem.Header -> (holder as HeaderVH).bind(item)
            is SearchItem.UserItem -> (holder as UserVH).bind(item)
            is SearchItem.TripItem -> (holder as TripVH).bind(item)
        }
    }

    inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: SearchItem.Header) {
            (itemView as TextView).text = item.title
        }
    }

    inner class UserVH(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: SearchItem.UserItem) {
            val img = itemView.findViewById<ImageView>(R.id.imgAvatar)
            val name = itemView.findViewById<TextView>(R.id.tvName)

            name.text = item.name
            Glide.with(itemView).load(item.avatar).into(img)

            itemView.setOnClickListener { onUserClick(item.userId) }
        }
    }

    inner class TripVH(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: SearchItem.TripItem) {
            val img = itemView.findViewById<ImageView>(R.id.imgTrip)
            val title = itemView.findViewById<TextView>(R.id.tvTitle)
            val tag = itemView.findViewById<TextView>(R.id.tvTag)

            title.text = item.title
            tag.text = item.tags
            Glide.with(itemView).load(item.image).into(img)

            itemView.setOnClickListener { onTripClick(item.tripId) }
        }
    }
}
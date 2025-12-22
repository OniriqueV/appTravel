package com.datn.apptravel.ui.discover.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.post.ImageUrlUtil
import com.datn.apptravel.ui.discover.util.TimeUtil

class ProfileTripAdapter(
    private val items: MutableList<DiscoverItem>,
    private val onTripClick: (String) -> Unit
) : RecyclerView.Adapter<ProfileTripAdapter.VH>() {

    fun submitList(list: List<DiscoverItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_trip, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgTrip: ImageView = itemView.findViewById(R.id.imgTrip)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(item: DiscoverItem) {
            tvCaption.text = item.caption.orEmpty()
            tvTime.text = TimeUtil.formatTimeAgo(item.sharedAt)

            Glide.with(itemView)
                .load(ImageUrlUtil.toFullUrl(item.tripImage))
                .placeholder(R.drawable.bg_trip_placeholder)
                .into(imgTrip)

            itemView.setOnClickListener {
                item.tripId?.let(onTripClick)
            }
        }
    }
}

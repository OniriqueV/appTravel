package com.datn.apptravels.ui.discover.post.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.model.TripItem
import com.datn.apptravels.ui.discover.util.ImageUrlUtil

class TripSelectAdapter(
    private val items: List<TripItem>,
    private val onClick: (TripItem) -> Unit
) : RecyclerView.Adapter<TripSelectAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.imgTrip)
        private val title: TextView = itemView.findViewById(R.id.tvTripTitle)

        fun bind(trip: TripItem) {
            title.text = trip.title ?: "Untitled trip"

            Glide.with(itemView)
                .load(ImageUrlUtil.toFullUrl(trip.coverPhoto))
                .placeholder(R.drawable.bg_trip_placeholder)
                .error(R.drawable.bg_trip_placeholder)
                .into(img)

            itemView.setOnClickListener { onClick(trip) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_select, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
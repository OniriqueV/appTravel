package com.datn.apptravel.ui.discover.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.TripItem

class TripSelectAdapter(
    private val items: List<TripItem>,
    private val onClick: (TripItem) -> Unit
) : RecyclerView.Adapter<TripSelectAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgTrip)
        val title: TextView = view.findViewById(R.id.tvTripTitle)

        fun bind(trip: TripItem) {
            title.text = trip.title ?: "(No title)"

            Glide.with(itemView)
                .load(trip.coverPhoto)
                .placeholder(R.drawable.bg_placeholder)
                .into(img)

            itemView.setOnClickListener { onClick(trip) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trip_select, parent, false)
        )
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
}

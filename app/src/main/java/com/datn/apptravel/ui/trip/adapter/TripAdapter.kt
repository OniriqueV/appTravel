package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ItemTripBinding
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.utils.ApiConfig

class TripAdapter(
    private var trips: List<Trip>,
    private val onTripClicked: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount(): Int = trips.size

    fun updateTrips(newTrips: List<Trip>) {
        trips = newTrips
        notifyDataSetChanged()
    }

    inner class TripViewHolder(private val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTripClicked(trips[position])
                }
            }
        }
        
        fun bind(trip: Trip) {
            binding.apply {
                tvTripName.text = trip.title
                tvTripStartDate.text = "Start: ${trip.startDate}"
                tvTripEndDate.text = "End: ${trip.endDate}"
                
                // Load cover photo if available
                val imageUrl = ApiConfig.getImageUrl(trip.coverPhoto)
                if (imageUrl != null) {
                    Glide.with(binding.root.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_a)
                        .error(R.drawable.bg_a)
                        .centerCrop()
                        .into(ivTripImage)
                } else {
                    ivTripImage.setImageResource(R.drawable.bg_a)
                }
            }
        }
    }
}
package com.datn.apptravel.ui.trip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.databinding.ItemTripBinding
import com.datn.apptravel.data.model.Trip

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
                
                // TODO: Load cover photo if available
                // Glide.with(binding.root.context)
                //     .load(trip.coverPhoto)
                //     .into(ivTripImage)
            }
        }
    }
}
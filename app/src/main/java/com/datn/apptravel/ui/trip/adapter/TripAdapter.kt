package com.datn.apptravels.ui.trip.adapter

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.databinding.ItemTripBinding
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.utils.ApiConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                
                // Show/hide warning badge based on conflict status
                ivWarningBadge.visibility = if (trip.hasConflict) View.VISIBLE else View.GONE
                
                // Set click listener for warning badge
                ivWarningBadge.setOnClickListener {
                    showConflictDialog()
                }
                
                // Format dates from yyyy-MM-dd to dd-MM-yyyy
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                
                val formattedStartDate = try {
                    val date = LocalDate.parse(trip.startDate, inputFormatter)
                    date.format(outputFormatter)
                } catch (e: Exception) {
                    trip.startDate
                }
                
                val formattedEndDate = try {
                    val date = LocalDate.parse(trip.endDate, inputFormatter)
                    date.format(outputFormatter)
                } catch (e: Exception) {
                    trip.endDate
                }
                
                tvTripStartDate.text = "Start: $formattedStartDate"
                tvTripEndDate.text = "End: $formattedEndDate"
                
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
        
        private fun showConflictDialog() {
            val dialog = Dialog(binding.root.context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_trip_conflict)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val ivClose = dialog.findViewById<android.widget.ImageView>(R.id.ivClose)
            ivClose.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }
}
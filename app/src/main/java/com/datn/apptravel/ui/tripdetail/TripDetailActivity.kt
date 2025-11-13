package com.datn.apptravel.ui.tripdetail

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ActivityTripDetailBinding
import com.datn.apptravel.ui.adapter.ScheduleDayAdapter
import com.datn.apptravel.ui.fragment.TripsFragment
import com.datn.apptravel.ui.planselection.PlanSelectionActivity
import com.datn.apptravel.ui.viewmodel.TripDetailViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class TripDetailActivity : AppCompatActivity() {
    
    private val viewModel: TripDetailViewModel by viewModel()
    private var tripId: String? = null
    private lateinit var binding: ActivityTripDetailBinding
    private lateinit var scheduleDayAdapter: ScheduleDayAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get trip ID from intent
        tripId = intent.getStringExtra(TripsFragment.EXTRA_TRIP_ID)
        
        setupUI()
        setupObservers()
        
        // Load trip details
        loadTripData()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Reload trip data when returning from other screens
        loadTripData()
    }
    
    private fun loadTripData() {
        tripId?.let { 
            viewModel.getTripDetails(it) 
        } ?: run {
            // If no trip ID, show empty state
            binding.emptyPlansContainer.visibility = View.VISIBLE
            binding.recyclerViewSchedule.visibility = View.GONE
        }
    }
    
    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup menu button for opening menu
        binding.btnMenu.setOnClickListener {
            showTripMenu()
        }
        
        // Setup add new plan button
        binding.btnAddNewPlan.setOnClickListener {
            navigateToPlanSelection()
        }
        
        // Setup save button
        binding.btnSaveTrip.setOnClickListener {
            // Toggle save state
            // For now, just show a toast
            android.widget.Toast.makeText(this, "Trip saved!", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // Setup schedule RecyclerView
        setupRecyclerView()
    }
    
    private fun setupRecyclerView() {
        scheduleDayAdapter = ScheduleDayAdapter(emptyList())
        binding.recyclerViewSchedule.apply {
            adapter = scheduleDayAdapter
            layoutManager = LinearLayoutManager(this@TripDetailActivity)
        }
    }
    
    private fun setupObservers() {
        // Observe trip details
        viewModel.tripDetails.observe(this) { trip ->
            updateUI(trip)
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // Observe schedule days
        viewModel.scheduleDays.observe(this) { scheduleDays ->
            if (scheduleDays.isNotEmpty()) {
                scheduleDayAdapter.updateScheduleDays(scheduleDays)
                binding.emptyPlansContainer.visibility = View.GONE
                binding.recyclerViewSchedule.visibility = View.VISIBLE
            } else {
                binding.emptyPlansContainer.visibility = View.VISIBLE
                binding.recyclerViewSchedule.visibility = View.GONE
            }
        }
    }

    private fun updateUI(trip: com.datn.apptravel.data.model.Trip?) {
        if (trip == null) {
            binding.tvTripName.text = ""
            binding.tvTripDate.text = ""
            return
        }
        
        // Set trip details from API
        binding.apply {
            tvTripName.text = trip.title ?: "Untitled Trip"
            
            // Format dates
            val startDate = trip.startDate?.toString() ?: "N/A"
            val endDate = trip.endDate?.toString() ?: "N/A"
            tvTripDate.text = "$startDate - $endDate"
            
            // Load cover photo if available
            if (!trip.coverPhoto.isNullOrEmpty()) {
                // TODO: Load image with Glide
                // Glide.with(this@TripDetailActivity).load(trip.coverPhoto).into(ivTripImage)
            }
        }
    }

    private fun showTripMenu() {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.trip_detail_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view_map -> {
                    // Open map view
                    navigateToMapView()
                    true
                }
                R.id.action_edit_trip -> {
                    // Open trip edit
                    android.widget.Toast.makeText(this, "Edit trip coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete_trip -> {
                    // Show delete confirmation
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the trip
                android.widget.Toast.makeText(this, "Trip deleted!", android.widget.Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToPlanSelection() {
        val intent = Intent(this, PlanSelectionActivity::class.java)
        intent.putExtra("tripId", tripId)
        startActivity(intent)
    }

    private fun navigateToMapView() {
        val intent = Intent(this, com.datn.apptravel.ui.tripmap.TripMapActivity::class.java)
        intent.putExtra("tripId", tripId)
        intent.putExtra("tripTitle", viewModel.tripDetails.value?.title ?: "Trip")
        startActivity(intent)
    }
}
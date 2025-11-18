package com.datn.apptravel.ui.trip

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.datn.apptravel.databinding.ActivityCreateTripBinding
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.CreateTripViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class CreateTripActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateTripBinding
    private val viewModel: CreateTripViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveTrip()
        }
        
        // Start date picker
        binding.etStartDate.setOnClickListener {
            showDatePicker { date ->
                binding.etStartDate.setText(date)
            }
        }
        
        // End date picker
        binding.etEndDate.setOnClickListener {
            showDatePicker { date ->
                binding.etEndDate.setText(date)
            }
        }
        
        // Upload image (TODO: implement image picker)
        binding.layoutUploadImage.setOnClickListener {
            // TODO: Open image picker
            Toast.makeText(this, "Image upload coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupObservers() {
        // Observe create trip result
        viewModel.createTripResult.observe(this) { trip ->
            if (trip != null) {
                Toast.makeText(this, "Trip created successfully!", Toast.LENGTH_SHORT).show()
                navigateToTripDetail(trip.id.toString())
            }
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "Saving..." else "Save"
        }
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            onDateSelected(formattedDate)
        }, year, month, day).show()
    }
    
    private fun saveTrip() {
        val tripName = binding.etTripName.text.toString().trim()
        val startDate = binding.etStartDate.text.toString().trim()
        val endDate = binding.etEndDate.text.toString().trim()
        
        // Validate inputs
        if (tripName.isEmpty()) {
            Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select end date", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Call ViewModel to create trip
        viewModel.createTrip(
            title = tripName,
            startDate = startDate,
            endDate = endDate,
            coverPhotoUri = null // TODO: Add cover photo URI when image upload is implemented
        )
    }

    private fun navigateToTripDetail(tripId: String) {
        val intent = Intent(this, TripDetailActivity::class.java)
        intent.putExtra(TripsFragment.EXTRA_TRIP_ID, tripId)
        startActivity(intent)
        finish()
    }
}
package com.datn.apptravel.ui.trip.detail.plandetail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.model.request.CreateBoatPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityBoatDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class BoatDetailActivity : AppCompatActivity() {
    
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private lateinit var binding: ActivityBoatDetailBinding
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        tripId = intent.getStringExtra("tripId")
        
        // Load trip dates
        tripId?.let { id ->
            lifecycleScope.launch {
                tripRepository.getTripById(id).onSuccess { trip ->
                    tripStartDate = trip.startDate
                    tripEndDate = trip.endDate
                }
            }
        }
        
    // Get place data from intent
    val placeName = intent.getStringExtra("placeName")
    val placeAddress = intent.getStringExtra("placeAddress")
    placeLatitude = intent.getDoubleExtra("placeLatitude", 0.0)
    placeLongitude = intent.getDoubleExtra("placeLongitude", 0.0)        // Pre-fill place data
        placeName?.let { binding.etBoatName.setText(it) }
        placeAddress?.let { binding.etDepartureLocation.setText(it) }
        
        setupUI()
    }
    
    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup save button
        binding.btnSave.setOnClickListener {
            saveBoatDetails()
        }
        
        // Setup date/time pickers
        setupDateTimePickers()
    }
    
    private fun setupDateTimePickers() {
        // Departure time picker (date + time combined)
        binding.etDepartureTime.setOnClickListener {
            showDateTimePicker(binding.etDepartureTime)
        }
        
        // Arrival time picker (date + time combined)
        binding.etArrivalTime.setOnClickListener {
            showDateTimePicker(binding.etArrivalTime)
        }
    }
    
    private fun showDateTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // After selecting date, show time picker
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedDateTime = String.format("%02d/%02d/%04d %02d:%02d", 
                    selectedDay, selectedMonth + 1, selectedYear, selectedHour, selectedMinute)
                targetEditText.setText(formattedDateTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, year, month, day).show()
    }
    
    private fun saveBoatDetails() {
        Log.d("BoatDetail", "========== SAVE BUTTON CLICKED ==========")
        
        // Validate inputs
        if (binding.etBoatName.text.isNullOrEmpty() ||
            binding.etDepartureTime.text.isNullOrEmpty() ||
            binding.etArrivalTime.text.isNullOrEmpty()) {
            Log.e("BoatDetail", "Validation failed - missing required fields")
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val departureDateTime = binding.etDepartureTime.text.toString()
            val arrivalDateTime = binding.etArrivalTime.text.toString()
            
            // Extract dates for validation
            val departureDate = departureDateTime.split(" ").firstOrNull() ?: ""
            val arrivalDate = arrivalDateTime.split(" ").firstOrNull() ?: ""
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(departureDate) || !isDateWithinTripRange(arrivalDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format (format: dd/MM/yyyy HH:mm)
            val startTimeISO = convertDateTimeStringToISO(departureDateTime)
            val endTimeISO = convertDateTimeStringToISO(arrivalDateTime)
            
            val request = CreateBoatPlanRequest(
                tripId = id,
                title = binding.etBoatName.text.toString(),
                address = binding.etDepartureLocation.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.BOAT.name,
                arrivalTime = endTimeISO,
                arrivalLocation = binding.etArrivalLocation.text.toString().takeIf { it.isNotEmpty() },
                arrivalAddress = null  // Not available in layout
            )
            
            Log.d("BoatDetail", "Creating boat plan for tripId: $id")
            Log.d("BoatDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createBoatPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("BoatDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@BoatDetailActivity, "Boat saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("BoatDetail", "Failed to create plan", exception)
                        Toast.makeText(this@BoatDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("BoatDetail", "Exception during plan creation", e)
                    Toast.makeText(this@BoatDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Trip ID is missing", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun convertDateTimeStringToISO(dateTimeString: String): String {
        // Convert "dd/MM/yyyy HH:mm" to "yyyy-MM-dd'T'HH:mm:ss"
        val parts = dateTimeString.split(" ")
        if (parts.size == 2) {
            val dateParts = parts[0].split("/")
            val time = parts[1]
            if (dateParts.size == 3) {
                val day = dateParts[0]
                val month = dateParts[1]
                val year = dateParts[2]
                return "$year-$month-${day}T$time:00"
            }
        }
        return ""
    }
    
    private fun isDateWithinTripRange(date: String): Boolean {
        if (tripStartDate == null || tripEndDate == null) return true
        
        try {
            // Convert dd/MM/yyyy to yyyy-MM-dd for comparison
            val parts = date.split("/")
            if (parts.size != 3) return true
            
            val planDate = "${parts[2]}-${parts[1]}-${parts[0]}"
            
            return planDate >= tripStartDate!! && planDate <= tripEndDate!!
        } catch (e: Exception) {
            return true
        }
    }
}
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
import com.datn.apptravel.data.model.request.CreateFlightPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityFlightDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class FlightDetailActivity : AppCompatActivity() {
    
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private lateinit var binding: ActivityFlightDetailBinding
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlightDetailBinding.inflate(layoutInflater)
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
        placeLongitude = intent.getDoubleExtra("placeLongitude", 0.0)
        
        // Pre-fill place data
        placeName?.let { binding.etAirline.setText(it) }
        placeAddress?.let { binding.etAddress.setText(it) }
        
        setupUI()
    }
    
    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup save button
        binding.btnSave.setOnClickListener {
            saveFlightDetails()
        }
        
        // Setup date/time pickers
        setupDateTimePickers()
    }
    
    private fun setupDateTimePickers() {
        // Departure date picker
        binding.etDepartureDate.setOnClickListener {
            showDatePicker(binding.etDepartureDate)
        }
        
        // Arrival date picker
        binding.etArrivalDate.setOnClickListener {
            showDatePicker(binding.etArrivalDate)
        }
        
        // Arrival time picker
        binding.etArrivalTime.setOnClickListener {
            showTimePicker(binding.etArrivalTime)
        }
    }
    
    private fun showDatePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            targetEditText.setText(formattedDate)
        }, year, month, day).show()
    }
    
    private fun showTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            targetEditText.setText(formattedTime)
        }, hour, minute, true).show()
    }

    private fun saveFlightDetails() {
        Log.d("FlightDetail", "========== SAVE BUTTON CLICKED ==========")
        
        // Validate inputs
        if (binding.etAirline.text.isNullOrEmpty() ||
            binding.etDepartureDate.text.isNullOrEmpty() ||
            binding.etArrivalDate.text.isNullOrEmpty() ||
            binding.etArrivalTime.text.isNullOrEmpty()) {
            Log.e("FlightDetail", "Validation failed - missing required fields")
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val departureDate = binding.etDepartureDate.text.toString()
            val departureTime = "09:00" // Default departure time
            val arrivalDate = binding.etArrivalDate.text.toString()
            val arrivalTime = binding.etArrivalTime.text.toString()
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(departureDate) || !isDateWithinTripRange(arrivalDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format
            val startTimeISO = convertDateTimeToISO(departureDate, departureTime)
            val endTimeISO = convertDateTimeToISO(arrivalDate, arrivalTime)
            
            val request = CreateFlightPlanRequest(
                tripId = id,
                title = binding.etAirline.text.toString(),
                address = binding.etAddress.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.FLIGHT.name,
                arrivalLocation = null,  // Not available in layout
                arrivalAddress = binding.etArrivalAddress.text.toString().takeIf { it.isNotEmpty() },
                arrivalDate = endTimeISO  // Same as arrival time
            )
            
            Log.d("FlightDetail", "Creating flight plan for tripId: $id")
            Log.d("FlightDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createFlightPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("FlightDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@FlightDetailActivity, "Flight saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("FlightDetail", "Failed to create plan", exception)
                        Toast.makeText(this@FlightDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("FlightDetail", "Exception during plan creation", e)
                    Toast.makeText(this@FlightDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Trip ID is missing", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun convertDateTimeToISO(date: String, time: String): String {
        // Convert dd/MM/yyyy and HH:mm to yyyy-MM-dd'T'HH:mm:ss
        val dateParts = date.split("/")
        if (dateParts.size == 3) {
            val day = dateParts[0]
            val month = dateParts[1]
            val year = dateParts[2]
            return "$year-$month-${day}T$time:00"
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

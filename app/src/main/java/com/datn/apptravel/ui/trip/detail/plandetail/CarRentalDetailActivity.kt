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
import com.datn.apptravel.data.model.request.CreateCarRentalPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityCarRentalDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class CarRentalDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCarRentalDetailBinding
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarRentalDetailBinding.inflate(layoutInflater)
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
        placeName?.let { binding.etRentalAgency.setText(it) }
        placeAddress?.let { binding.etPickupLocation.setText(it) }
        
        setupUI()
    }
    
    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup save button
        binding.btnSave.setOnClickListener {
            saveCarRentalDetails()
        }
        
        // Setup date/time pickers
        setupDateTimePickers()
    }
    
    private fun setupDateTimePickers() {
        // Pickup date picker
        binding.etPickupDate.setOnClickListener {
            showDatePicker(binding.etPickupDate)
        }
        
        // Pickup time picker
        binding.etPickupTime.setOnClickListener {
            showTimePicker(binding.etPickupTime)
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

    private fun saveCarRentalDetails() {
        Log.d("CarRentalDetail", "========== SAVE BUTTON CLICKED ==========")
        
        // Validate inputs
        if (binding.etRentalAgency.text.isNullOrEmpty() ||
            binding.etPickupDate.text.isNullOrEmpty() ||
            binding.etPickupTime.text.isNullOrEmpty()) {
            Log.e("CarRentalDetail", "Validation failed - missing required fields")
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val pickupDate = binding.etPickupDate.text.toString()
            val pickupTime = binding.etPickupTime.text.toString()
            
            // Use default return date/time (3 days later, same time)
            val returnDate = addDays(pickupDate, 3)
            val returnTime = pickupTime
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(pickupDate) || !isDateWithinTripRange(returnDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format
            val startTimeISO = convertDateTimeToISO(pickupDate, pickupTime)
            val endTimeISO = convertDateTimeToISO(returnDate, returnTime)
            
            val request = CreateCarRentalPlanRequest(
                tripId = id,
                title = binding.etRentalAgency.text.toString(),
                address = binding.etPickupLocation.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.CAR_RENTAL.name,
                pickupDate = startTimeISO,
                pickupTime = startTimeISO,
                phone = binding.etPhone.text.toString().takeIf { it.isNotEmpty() }
            )
            
            Log.d("CarRentalDetail", "Creating car rental plan for tripId: $id")
            Log.d("CarRentalDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createCarRentalPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("CarRentalDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@CarRentalDetailActivity, "Car rental saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("CarRentalDetail", "Failed to create plan", exception)
                        Toast.makeText(this@CarRentalDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CarRentalDetail", "Exception during plan creation", e)
                    Toast.makeText(this@CarRentalDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
    
    private fun addDays(date: String, days: Int): String {
        // Simple date addition (dd/MM/yyyy + days)
        val dateParts = date.split("/")
        if (dateParts.size == 3) {
            val day = dateParts[0].toInt() + days
            val month = dateParts[1]
            val year = dateParts[2]
            return String.format("%02d/%s/%s", day, month, year)
        }
        return date
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

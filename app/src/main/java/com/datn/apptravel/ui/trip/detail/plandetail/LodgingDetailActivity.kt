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
import com.datn.apptravel.data.model.request.CreateLodgingPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityLodgingDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class LodgingDetailActivity : AppCompatActivity() {
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private lateinit var binding: ActivityLodgingDetailBinding
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLodgingDetailBinding.inflate(layoutInflater)
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
        placeName?.let { binding.etLodgingName.setText(it) }
        placeAddress?.let { binding.etAddress.setText(it) }
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Setup save button
        binding.btnSave.setOnClickListener {
            saveLodgingDetails()
        }
        
        // Setup date pickers
        setupDatePickers()
        
        // Setup time pickers
        setupTimePickers()
    }
    
    private fun setupObservers() {

    }
    
    private fun setupDatePickers() {
        // Check-in date picker
        binding.etCheckInDate.setOnClickListener {
            showDatePicker(binding.etCheckInDate)
        }
        
        // Check-out date picker
        binding.etCheckoutDate.setOnClickListener {
            showDatePicker(binding.etCheckoutDate)
        }
    }
    
    private fun setupTimePickers() {
        // Check-in time picker
        binding.etCheckInTime.setOnClickListener {
            showTimePicker(binding.etCheckInTime)
        }
        
        // Check-out time picker
        binding.etCheckoutTime.setOnClickListener {
            showTimePicker(binding.etCheckoutTime)
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

    private fun saveLodgingDetails() {
        Log.d("LodgingDetail", "========== SAVE BUTTON CLICKED ==========")
        
        // Validate inputs
        if (binding.etLodgingName.text.isNullOrEmpty() || 
            binding.etCheckInDate.text.isNullOrEmpty() ||
            binding.etCheckInTime.text.isNullOrEmpty() ||
            binding.etCheckoutDate.text.isNullOrEmpty() ||
            binding.etCheckoutTime.text.isNullOrEmpty()) {
            Log.e("LodgingDetail", "Validation failed - missing required fields")
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val checkInDate = binding.etCheckInDate.text.toString()
            val checkInTime = binding.etCheckInTime.text.toString()
            val checkoutDate = binding.etCheckoutDate.text.toString()
            val checkoutTime = binding.etCheckoutTime.text.toString()
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(checkInDate) || !isDateWithinTripRange(checkoutDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format
            val startTimeISO = convertDateTimeToISO(checkInDate, checkInTime)
            val endTimeISO = convertDateTimeToISO(checkoutDate, checkoutTime)
            
            val request = CreateLodgingPlanRequest(
                tripId = id,
                title = binding.etLodgingName.text.toString(),
                address = binding.etAddress.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.LODGING.name,
                checkInDate = startTimeISO,
                checkOutDate = endTimeISO,
                phone = binding.etPhone.text.toString().takeIf { it.isNotEmpty() }
            )
            
            Log.d("LodgingDetail", "Creating lodging plan for tripId: $id")
            Log.d("LodgingDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createLodgingPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("LodgingDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@LodgingDetailActivity, "Lodging saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("LodgingDetail", "Failed to create plan", exception)
                        Toast.makeText(this@LodgingDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LodgingDetail", "Exception during plan creation", e)
                    Toast.makeText(this@LodgingDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
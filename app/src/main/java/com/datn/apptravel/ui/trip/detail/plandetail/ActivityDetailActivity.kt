package com.datn.apptravel.ui.trip.detail.plandetail

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.model.request.CreateActivityPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityActivityDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class ActivityDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityActivityDetailBinding
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityDetailBinding.inflate(layoutInflater)
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
        placeName?.let { binding.etEventName.setText(it) }
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
            saveActivityDetails()
        }
        
        // Setup date pickers
        binding.etStartTime.setOnClickListener {
            showDatePicker(binding.etStartTime)
        }
        
        binding.etEndTime.setOnClickListener {
            showDatePicker(binding.etEndTime)
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

    private fun saveActivityDetails() {
        // Validate inputs
        if (binding.etEventName.text.isNullOrEmpty() ||
            binding.etStartTime.text.isNullOrEmpty() ||
            binding.etEndTime.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val startDate = binding.etStartTime.text.toString()
            val endDate = binding.etEndTime.text.toString()
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(startDate) || !isDateWithinTripRange(endDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format (assuming time is 12:00)
            val startTimeISO = convertDateToISO(startDate, "12:00")
            val endTimeISO = convertDateToISO(endDate, "18:00")
            
            val request = CreateActivityPlanRequest(
                tripId = id,
                title = binding.etEventName.text.toString(),
                address = binding.etAddress.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.ACTIVITY.name
            )
            
            Log.d("ActivityDetail", "Creating activity plan for tripId: $id")
            Log.d("ActivityDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createActivityPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("ActivityDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@ActivityDetailActivity, "Activity saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("ActivityDetail", "Failed to create plan", exception)
                        Toast.makeText(this@ActivityDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ActivityDetail", "Exception during plan creation", e)
                    Toast.makeText(this@ActivityDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Trip ID is missing", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun convertDateToISO(date: String, time: String): String {
        // Convert dd/MM/yyyy and HH:mm to yyyy-MM-dd'T'HH:mm:ss
        val parts = date.split("/")
        if (parts.size == 3) {
            val day = parts[0]
            val month = parts[1]
            val year = parts[2]
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

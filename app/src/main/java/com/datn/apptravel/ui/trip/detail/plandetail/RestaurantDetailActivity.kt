package com.datn.apptravel.ui.trip.detail.plandetail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.model.request.CreateRestaurantPlanRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityRestaurantDetailBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class RestaurantDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRestaurantDetailBinding
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private val tripRepository: TripRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantDetailBinding.inflate(layoutInflater)
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
        placeName?.let { binding.etRestaurantName.setText(it) }
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
            saveRestaurantDetails()
        }
        
        // Setup date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        // Setup time picker
        binding.etTime.setOnClickListener {
            showTimePicker()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            binding.etDate.setText(formattedDate)
        }, year, month, day).show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            binding.etTime.setText(formattedTime)
        }, hour, minute, true).show()
    }

    private fun saveRestaurantDetails() {
        // Validate inputs
        if (binding.etRestaurantName.text.isNullOrEmpty() ||
            binding.etDate.text.isNullOrEmpty() ||
            binding.etTime.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val date = binding.etDate.text.toString()
            val time = binding.etTime.text.toString()
            
            // Validate date is within trip dates
            if (!isDateWithinTripRange(date)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Combine date and time to ISO format
            val startTimeISO = convertToISO(date, time)
            val endTimeISO = convertToISO(date, addOneHour(time))
            
            val request = CreateRestaurantPlanRequest(
                tripId = id,
                title = binding.etRestaurantName.text.toString(),
                address = binding.etAddress.text.toString(),
                location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                    "$placeLatitude,$placeLongitude"
                } else null,
                startTime = startTimeISO,
                endTime = endTimeISO,
                expense = binding.etExpense.text.toString().toDoubleOrNull(),
                photoUrl = null,
                type = PlanType.RESTAURANT.name,
                reservationDate = startTimeISO,
                reservationTime = startTimeISO
            )
            
            Log.d("RestaurantDetail", "Creating restaurant plan for tripId: $id")
            Log.d("RestaurantDetail", "Request: $request")
            
            lifecycleScope.launch {
                try {
                    val result = tripRepository.createRestaurantPlan(id, request)
                    result.onSuccess { plan ->
                        Log.d("RestaurantDetail", "Plan created successfully: ${plan.id}")
                        Toast.makeText(this@RestaurantDetailActivity, "Restaurant saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("RestaurantDetail", "Failed to create plan", exception)
                        Toast.makeText(this@RestaurantDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RestaurantDetail", "Exception during plan creation", e)
                    Toast.makeText(this@RestaurantDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "Trip ID is missing", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun convertToISO(date: String, time: String): String {
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
    
    private fun addOneHour(time: String): String {
        val parts = time.split(":")
        if (parts.size == 2) {
            var hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1]
            hour = (hour + 1) % 24
            return String.format("%02d:%s", hour, minute)
        }
        return time
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

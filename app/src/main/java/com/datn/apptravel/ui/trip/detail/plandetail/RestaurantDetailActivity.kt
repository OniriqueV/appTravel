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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Calendar

class RestaurantDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRestaurantDetailBinding
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private var photoUrl: String? = null
    private val tripRepository: TripRepository by inject()
    
    private var isEditMode = false
    private var planId: String? = null
    
    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_PLAN_TITLE = "plan_title"
        const val EXTRA_PLACE_ADDRESS = "place_address"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_EXPENSE = "expense"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if in edit mode
        planId = intent.getStringExtra(EXTRA_PLAN_ID)
        isEditMode = planId != null
        
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
        photoUrl = intent.getStringExtra("photoUrl")
        
        // Pre-fill place data
        placeName?.let { binding.etRestaurantName.setText(it) }
        placeAddress?.let { binding.etAddress.setText(it) }
        
        // Load edit data if in edit mode
        if (isEditMode) {
            loadEditData()
            // Disable place name and address in edit mode
            binding.etRestaurantName.isEnabled = false
            binding.etAddress.isEnabled = false
        }
        
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
            
            lifecycleScope.launch {
                try {
                    // Download and upload image if photoUrl is a URL (starts with http)
                    var uploadedFilename: String? = null
                    if (!photoUrl.isNullOrEmpty() && photoUrl!!.startsWith("http")) {
                        Log.d("RestaurantDetail", "Downloading and uploading image from URL: $photoUrl")
                        val uploadResult = withContext(Dispatchers.IO) {
                            tripRepository.downloadAndUploadImage(this@RestaurantDetailActivity, photoUrl!!)
                        }
                        uploadResult.onSuccess { filename ->
                            uploadedFilename = filename
                            Log.d("RestaurantDetail", "Image uploaded successfully: $filename")
                        }.onFailure { exception ->
                            Log.e("RestaurantDetail", "Failed to upload image: ${exception.message}", exception)
                        }
                    } else {
                        // If photoUrl is already a filename, use it
                        uploadedFilename = photoUrl
                    }
                    
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
                        photoUrl = uploadedFilename,
                        type = PlanType.RESTAURANT.name,
                        reservationDate = startTimeISO,
                        reservationTime = startTimeISO
                    )
                    
                    Log.d("RestaurantDetail", "Creating restaurant plan for tripId: $id")
                    Log.d("RestaurantDetail", "Request: $request")
            
                    val result = if (isEditMode && planId != null) {
                        // Update existing plan
                        tripRepository.updateRestaurantPlan(id, planId!!, request)
                    } else {
                        // Create new plan
                        tripRepository.createRestaurantPlan(id, request)
                    }
                    
                    result.onSuccess { plan ->
                        Log.d("RestaurantDetail", "Plan saved successfully: ${plan.id}")
                        Toast.makeText(this@RestaurantDetailActivity, "Restaurant saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("RestaurantDetail", "Failed to save plan", exception)
                        Toast.makeText(this@RestaurantDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RestaurantDetail", "Exception during plan save", e)
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
    
    private fun loadEditData() {
        val title = intent.getStringExtra(EXTRA_PLAN_TITLE)
        val address = intent.getStringExtra(EXTRA_PLACE_ADDRESS)
        val startTime = intent.getStringExtra(EXTRA_START_TIME)
        val expense = intent.getDoubleExtra(EXTRA_EXPENSE, 0.0)
        
        // Pre-fill form
        title?.let { binding.etRestaurantName.setText(it) }
        address?.let { binding.etAddress.setText(it) }
        
        // Parse and set date and time from ISO format
        startTime?.let { isoTime ->
            try {
                // Parse yyyy-MM-dd'T'HH:mm:ss to dd/MM/yyyy and HH:mm
                val parts = isoTime.split("T")
                if (parts.size == 2) {
                    val datePart = parts[0] // yyyy-MM-dd
                    val timePart = parts[1].substring(0, 5) // HH:mm
                    
                    val dateParts = datePart.split("-")
                    if (dateParts.size == 3) {
                        val formattedDate = String.format("%s/%s/%s", dateParts[2], dateParts[1], dateParts[0])
                        binding.etDate.setText(formattedDate)
                    }
                    binding.etTime.setText(timePart)
                }
            } catch (e: Exception) {
                Log.e("RestaurantDetail", "Error parsing start time", e)
            }
        }
        
        if (expense > 0) {
            binding.etExpense.setText(expense.toString())
        }
    }
}

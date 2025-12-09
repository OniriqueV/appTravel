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
    
    private var isEditMode = false
    private var planId: String? = null
    
    // Store selected date and time separately
    private var startDate: String = ""
    private var startTime: String = ""
    private var endDate: String = ""
    private var endTime: String = ""
    
    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_PLAN_TITLE = "plan_title"
        const val EXTRA_PLACE_ADDRESS = "place_address"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_EXPENSE = "expense"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityDetailBinding.inflate(layoutInflater)
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
        
        // Pre-fill place data
        placeName?.let { binding.etEventName.setText(it) }
        placeAddress?.let { binding.etAddress.setText(it) }
        
        // Load edit data if in edit mode
        if (isEditMode) {
            loadEditData()
            // Disable place name and address in edit mode
            binding.etEventName.isEnabled = false
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
            saveActivityDetails()
        }
        
        // Setup date pickers
        binding.etStartDate.setOnClickListener {
            showDatePicker(true, isDatePicker = true)
        }
        
        binding.etEndDate.setOnClickListener {
            showDatePicker(false, isDatePicker = true)
        }
        
        // Setup time pickers
        binding.etStartTime.setOnClickListener {
            showTimePicker(true)
        }
        
        binding.etEndTime.setOnClickListener {
            showTimePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDateTime: Boolean, isDatePicker: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
            
            if (isStartDateTime) {
                startDate = formattedDate
                binding.etStartDate.setText(formattedDate)
            } else {
                endDate = formattedDate
                binding.etEndDate.setText(formattedDate)
            }
        }, year, month, day).show()
    }
    
    private fun showTimePicker(isStartDateTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(this, { _, hourOfDay, minute ->
            val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
            
            if (isStartDateTime) {
                startTime = formattedTime
                binding.etStartTime.setText(formattedTime)
            } else {
                endTime = formattedTime
                binding.etEndTime.setText(formattedTime)
            }
        }, currentHour, currentMinute, true).show()
    }

    private fun saveActivityDetails() {
        // Validate inputs - ALL fields are required
        if (binding.etEventName.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter event name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select start date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startTime.isEmpty()) {
            Toast.makeText(this, "Please select start time", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select end date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (endTime.isEmpty()) {
            Toast.makeText(this, "Please select end time", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(startDate) || !isDateWithinTripRange(endDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format with selected time
            val startTimeISO = convertDateTimeToISO(startDate, startTime)
            val endTimeISO = convertDateTimeToISO(endDate, endTime)
            
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
                    val result = if (isEditMode && planId != null) {
                        tripRepository.updateActivityPlan(id, planId!!, request)
                    } else {
                        tripRepository.createActivityPlan(id, request)
                    }
                    
                    result.onSuccess { plan ->
                        Log.d("ActivityDetail", "Plan saved successfully: ${plan.id}")
                        Toast.makeText(this@ActivityDetailActivity, "Activity saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }.onFailure { exception ->
                        Log.e("ActivityDetail", "Failed to save plan", exception)
                        Toast.makeText(this@ActivityDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ActivityDetail", "Exception during plan save", e)
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
    
    private fun convertDateTimeToISO(date: String, time: String): String {
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
    
    private fun loadEditData() {
        val title = intent.getStringExtra(EXTRA_PLAN_TITLE)
        val address = intent.getStringExtra(EXTRA_PLACE_ADDRESS)
        val startTimeISO = intent.getStringExtra(EXTRA_START_TIME)
        val endTimeISO = intent.getStringExtra(EXTRA_END_TIME)
        val expense = intent.getDoubleExtra(EXTRA_EXPENSE, 0.0)
        
        title?.let { binding.etEventName.setText(it) }
        address?.let { binding.etAddress.setText(it) }
        
        startTimeISO?.let { isoTime ->
            try {
                val parts = isoTime.split("T")
                if (parts.size == 2) {
                    val datePart = parts[0]
                    val timePart = parts[1].substring(0, 5) // Get HH:mm
                    val dateParts = datePart.split("-")
                    if (dateParts.size == 3) {
                        startDate = String.format("%s/%s/%s", dateParts[2], dateParts[1], dateParts[0])
                        startTime = timePart
                        binding.etStartDate.setText(startDate)
                        binding.etStartTime.setText(startTime)
                    }
                }
            } catch (e: Exception) {
                Log.e("ActivityDetail", "Error parsing start time", e)
            }
        }
        
        endTimeISO?.let { isoTime ->
            try {
                val parts = isoTime.split("T")
                if (parts.size == 2) {
                    val datePart = parts[0]
                    val timePart = parts[1].substring(0, 5) // Get HH:mm
                    val dateParts = datePart.split("-")
                    if (dateParts.size == 3) {
                        endDate = String.format("%s/%s/%s", dateParts[2], dateParts[1], dateParts[0])
                        endTime = timePart
                        binding.etEndDate.setText(endDate)
                        binding.etEndTime.setText(endTime)
                    }
                }
            } catch (e: Exception) {
                Log.e("ActivityDetail", "Error parsing end time", e)
            }
        }
        
        if (expense > 0) {
            binding.etExpense.setText(expense.toString())
        }
    }
}

package com.datn.apptravels.ui.trip.detail.plandetail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravels.data.model.PlanType
import com.datn.apptravels.data.model.request.CreateBoatPlanRequest
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.databinding.ActivityBoatDetailBinding
import com.datn.apptravels.utils.ExpenseFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Calendar

class BoatDetailActivity : AppCompatActivity() {
    
    private var tripId: String? = null
    private var tripStartDate: String? = null
    private var tripEndDate: String? = null
    private var placeLatitude: Double = 0.0
    private var placeLongitude: Double = 0.0
    private var photoUrl: String? = null
    private lateinit var binding: ActivityBoatDetailBinding
    private val tripRepository: TripRepository by inject()
    
    private var isEditMode = false
    private var planId: String? = null
    
    // Separate date and time variables
    private var departureDate: String = ""
    private var departureTime: String = ""
    private var arrivalDate: String = ""
    private var arrivalTime: String = ""
    
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
        binding = ActivityBoatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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
        placeName?.let { binding.etBoatName.setText(it) }
        placeAddress?.let { binding.etDepartureLocation.setText(it) }
        
        if (isEditMode) {
            loadEditData()
            binding.etBoatName.isEnabled = false
            binding.etDepartureLocation.isEnabled = false
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
            saveBoatDetails()
        }
        
        // Setup date/time pickers
        setupDateTimePickers()
        
        // Setup expense formatter
        ExpenseFormatter.addExpenseFormatter(binding.etExpense)
    }
    
    private fun setupDateTimePickers() {
        // Departure date picker
        binding.etDepartureDate.setOnClickListener {
            showDatePicker(binding.etDepartureDate)
        }
        
        // Departure time picker
        binding.etDepartureTime.setOnClickListener {
            showTimePicker(binding.etDepartureTime)
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
    
    private fun saveBoatDetails() {
        Log.d("BoatDetail", "========== SAVE BUTTON CLICKED ==========")
        
        // Validate inputs
        if (binding.etBoatName.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter boat name", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.etDepartureDate.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select departure date", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.etDepartureTime.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select departure time", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.etArrivalDate.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select arrival date", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.etArrivalTime.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select arrival time", Toast.LENGTH_SHORT).show()
            return
        }
        
        tripId?.let { id ->
            val departureDate = binding.etDepartureDate.text.toString()
            val departureTime = binding.etDepartureTime.text.toString()
            val arrivalDate = binding.etArrivalDate.text.toString()
            val arrivalTime = binding.etArrivalTime.text.toString()
            
            // Validate dates are within trip dates
            if (!isDateWithinTripRange(departureDate) || !isDateWithinTripRange(arrivalDate)) {
                Toast.makeText(this, "Ngoài thời gian của chuyến đi", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validate that arrival datetime is after departure datetime
            if (!isArrivalAfterDeparture(departureDate, departureTime, arrivalDate, arrivalTime)) {
                Toast.makeText(this, "Thời gian đến phải sau thời gian khởi hành", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Convert to ISO format
            val startTimeISO = convertDateTimeToISO(departureDate, departureTime)
            val endTimeISO = convertDateTimeToISO(arrivalDate, arrivalTime)
            
            lifecycleScope.launch {
                try {
                    // Download and upload image if photoUrl is a URL (starts with http)
                    var uploadedFilename: String? = null
                    if (!photoUrl.isNullOrEmpty() && photoUrl!!.startsWith("http")) {
                        Log.d("BoatDetail", "Downloading and uploading image from URL: $photoUrl")
                        val uploadResult = withContext(Dispatchers.IO) {
                            tripRepository.downloadAndUploadImage(this@BoatDetailActivity, photoUrl!!)
                        }
                        uploadResult.onSuccess { filename ->
                            uploadedFilename = filename
                            Log.d("BoatDetail", "Image uploaded successfully: $filename")
                        }.onFailure { exception ->
                            Log.e("BoatDetail", "Failed to upload image: ${exception.message}", exception)
                        }
                    } else {
                        // If photoUrl is already a filename, use it
                        uploadedFilename = photoUrl
                    }
            
                    val request = CreateBoatPlanRequest(
                        tripId = id,
                        title = binding.etBoatName.text.toString(),
                        address = binding.etDepartureLocation.text.toString(),
                        location = if (placeLatitude != 0.0 && placeLongitude != 0.0) {
                            "$placeLatitude,$placeLongitude"
                        } else null,
                        startTime = startTimeISO,
                        endTime = endTimeISO,
                        expense = ExpenseFormatter.parseExpense(binding.etExpense.text.toString()),
                        photoUrl = uploadedFilename,
                        type = PlanType.BOAT.name,
                        arrivalTime = endTimeISO,
                        arrivalLocation = binding.etArrivalLocation.text.toString().takeIf { it.isNotEmpty() },
                        arrivalAddress = null  // Not available in layout
                    )
                    
                    Log.d("BoatDetail", "Creating boat plan for tripId: $id")
                    Log.d("BoatDetail", "Request: $request")
            
                    val result = if (isEditMode && planId != null) {
                        tripRepository.updateBoatPlan(id, planId!!, request)
                    } else {
                        tripRepository.createBoatPlan(id, request)
                    }
                    
                    result.onSuccess { plan ->
                        Log.d("BoatDetail", "Plan saved successfully: ${plan.id}")
                        Toast.makeText(this@BoatDetailActivity, "Boat saved", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }.onFailure { exception ->
                        Log.e("BoatDetail", "Failed to save plan", exception)
                        Toast.makeText(this@BoatDetailActivity, exception.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("BoatDetail", "Exception during plan save", e)
                    Toast.makeText(this@BoatDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
    
    private fun loadEditData() {
        val title = intent.getStringExtra(EXTRA_PLAN_TITLE)
        val address = intent.getStringExtra(EXTRA_PLACE_ADDRESS)
        val startTime = intent.getStringExtra(EXTRA_START_TIME)
        val endTime = intent.getStringExtra(EXTRA_END_TIME)
        val expense = intent.getDoubleExtra(EXTRA_EXPENSE, 0.0)
        
        title?.let { binding.etBoatName.setText(it) }
        address?.let { binding.etDepartureLocation.setText(it) }
        
        startTime?.let { isoTime ->
            try {
                val parts = isoTime.split("T")
                if (parts.size == 2) {
                    val datePart = parts[0]
                    val timePart = parts[1].substring(0, 5)
                    val dateParts = datePart.split("-")
                    if (dateParts.size == 3) {
                        val formattedDate = String.format("%s/%s/%s", dateParts[2], dateParts[1], dateParts[0])
                        binding.etDepartureDate.setText(formattedDate)
                        binding.etDepartureTime.setText(timePart)
                    }
                }
            } catch (e: Exception) {
                Log.e("BoatDetail", "Error parsing start time", e)
            }
        }
        
        endTime?.let { isoTime ->
            try {
                val parts = isoTime.split("T")
                if (parts.size == 2) {
                    val datePart = parts[0]
                    val timePart = parts[1].substring(0, 5)
                    val dateParts = datePart.split("-")
                    if (dateParts.size == 3) {
                        val formattedDate = String.format("%s/%s/%s", dateParts[2], dateParts[1], dateParts[0])
                        binding.etArrivalDate.setText(formattedDate)
                        binding.etArrivalTime.setText(timePart)
                    }
                }
            } catch (e: Exception) {
                Log.e("BoatDetail", "Error parsing end time", e)
            }
        }
        
        if (expense > 0) {
            binding.etExpense.setText(ExpenseFormatter.formatExpense(expense))
        }
    }
    
    private fun isArrivalAfterDeparture(departureDate: String, departureTime: String, arrivalDate: String, arrivalTime: String): Boolean {
        return try {
            val departureDateTime = convertDateTimeToISO(departureDate, departureTime)
            val arrivalDateTime = convertDateTimeToISO(arrivalDate, arrivalTime)
            
            arrivalDateTime > departureDateTime
        } catch (e: Exception) {
            true // If parsing fails, allow the operation
        }
    }
}
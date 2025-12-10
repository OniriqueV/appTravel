package com.datn.apptravel.ui.trip

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.databinding.ActivityCreateTripBinding
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.CreateTripViewModel
import com.datn.apptravel.utils.ApiConfig
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class CreateTripActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateTripBinding
    private val viewModel: CreateTripViewModel by viewModel()
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var tripId: String? = null
    private var existingCoverPhoto: String? = null
    
    companion object {
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_TRIP_TITLE = "trip_title"
        const val EXTRA_START_DATE = "start_date"
        const val EXTRA_END_DATE = "end_date"
        const val EXTRA_COVER_PHOTO = "cover_photo"
    }
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showImagePreview(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if in edit mode
        tripId = intent.getStringExtra(EXTRA_TRIP_ID)
        isEditMode = tripId != null
        
        setupUI()
        setupObservers()
        loadEditData()
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
            imagePickerLauncher.launch("image/*")
        }
    }
    
    private fun showImagePreview(uri: Uri) {
        binding.ivCoverPreview.setImageURI(uri)
        binding.ivCoverPreview.visibility = View.VISIBLE
        binding.layoutUploadPlaceholder.visibility = View.GONE
    }
    
    private fun setupObservers() {
        // Observe create trip result
        viewModel.createTripResult.observe(this) { trip ->
            if (trip != null && isEditMode !=true) {
                Toast.makeText(this, "Trip created successfully!", Toast.LENGTH_SHORT).show()

            }else{
                Toast.makeText(this, "Change information successfully!", Toast.LENGTH_SHORT).show()

            }
            navigateToTripDetail(trip?.id.toString())
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
        
        // Upload image first if selected
        if (selectedImageUri != null) {
            // Store trip data in ViewModel (including tripId if editing)
            viewModel.setPendingTripData(
                tripName, 
                startDate, 
                endDate, 
                if (isEditMode) tripId else null
            )
            
            // Upload image
            lifecycleScope.launch {
                viewModel.uploadCoverPhoto(this@CreateTripActivity, selectedImageUri!!)
            }
        } else {
            if (isEditMode && tripId != null) {
                // Update existing trip
                viewModel.updateTrip(
                    tripId = tripId!!,
                    title = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    coverPhotoUri = existingCoverPhoto
                )
            } else {
                // Create new trip
                viewModel.createTrip(
                    title = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    coverPhotoUri = null
                )
            }
        }
    }

    private fun loadEditData() {
        if (isEditMode) {
            // Load existing trip data from intent
            val title = intent.getStringExtra(EXTRA_TRIP_TITLE)
            val startDate = intent.getStringExtra(EXTRA_START_DATE)
            val endDate = intent.getStringExtra(EXTRA_END_DATE)
            existingCoverPhoto = intent.getStringExtra(EXTRA_COVER_PHOTO)
            
            // Pre-fill form
            title?.let { binding.etTripName.setText(it) }
            startDate?.let { binding.etStartDate.setText(it) }
            endDate?.let { binding.etEndDate.setText(it) }
            
            // Load cover photo if available
            existingCoverPhoto?.let { photoUrl ->
                val imageUrl = ApiConfig.getImageUrl(photoUrl)
                if (imageUrl != null) {
                    binding.ivCoverPreview.visibility = View.VISIBLE
                    binding.layoutUploadPlaceholder.visibility = View.GONE
                    com.bumptech.glide.Glide.with(this)
                        .load(imageUrl)
                        .into(binding.ivCoverPreview)
                }
            }
        }
    }
    
    private fun navigateToTripDetail(tripId: String) {
        if (isEditMode) {
            // In edit mode, just close and let TripDetailActivity reload
            finish()
        } else {
            // In create mode, navigate to new trip detail
            val intent = Intent(this, TripDetailActivity::class.java)
            intent.putExtra(TripsFragment.EXTRA_TRIP_ID, tripId)
            startActivity(intent)
            finish()
        }
    }
}
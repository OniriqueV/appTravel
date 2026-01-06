package com.datn.apptravels.ui.trip

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravels.data.model.User
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.databinding.ActivityCreateTripBinding
import com.datn.apptravels.databinding.DialogSelectFollowersBinding
import com.datn.apptravels.ui.trip.adapter.FollowerSelectionAdapter
import com.datn.apptravels.ui.trip.adapter.TripMemberAdapter
import com.datn.apptravels.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravels.ui.trip.viewmodel.CreateTripViewModel
import com.datn.apptravels.utils.ApiConfig
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class CreateTripActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateTripBinding
    private val viewModel: CreateTripViewModel by viewModel()
    private val tripRepository: TripRepository by inject()
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var tripId: String? = null
    private var existingCoverPhoto: String? = null
    
    // Member selection
    private val selectedMembers = mutableListOf<User>()
    private lateinit var memberAdapter: TripMemberAdapter
    private val auth = FirebaseAuth.getInstance()
    
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
        // Setup member RecyclerView
        memberAdapter = TripMemberAdapter { user ->
            removeMember(user)
        }
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@CreateTripActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = memberAdapter
        }
        
        // Add member button
        binding.btnAddMember.setOnClickListener {
            showFollowerSelectionDialog()
        }
        
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
        
        // Validate that end date is after start date
        if (!isEndDateAfterStartDate(startDate, endDate)) {
            Toast.makeText(this, "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Upload image first if selected
        if (selectedImageUri != null) {
            // Store trip data in ViewModel (including tripId if editing)
            viewModel.setPendingTripData(
                tripName, 
                startDate, 
                endDate, 
                if (isEditMode) tripId else null,
                if (selectedMembers.isNotEmpty()) selectedMembers else null
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
                    coverPhotoUri = existingCoverPhoto,
                    members = if (selectedMembers.isNotEmpty()) selectedMembers else null
                )
            } else {
                // Create new trip with members
                viewModel.createTrip(
                    title = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    coverPhotoUri = null,
                    members = if (selectedMembers.isNotEmpty()) selectedMembers else null
                )
            }
        }
    }

    private fun loadEditData() {
        if (isEditMode && tripId != null) {
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
            
            // Load members from backend
            lifecycleScope.launch {
                try {
                    val result = tripRepository.getTripById(tripId!!)
                    result.onSuccess { trip ->
                        if (!trip.members.isNullOrEmpty()) {
                            selectedMembers.clear()
                            selectedMembers.addAll(trip.members)
                            // Create new list to trigger DiffUtil
                            memberAdapter.submitList(ArrayList(selectedMembers))
                            android.util.Log.d("CreateTripActivity", "Loaded ${trip.members.size} existing members")
                        }
                    }.onFailure { exception ->
                        android.util.Log.e("CreateTripActivity", "Failed to load trip members: ${exception.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CreateTripActivity", "Error loading trip members: ${e.message}")
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
    
    private fun isEndDateAfterStartDate(startDate: String, endDate: String): Boolean {
        return try {
            val parts1 = startDate.split("/")
            val parts2 = endDate.split("/")
            
            if (parts1.size != 3 || parts2.size != 3) return true
            
            // Convert dd/MM/yyyy to yyyy-MM-dd for comparison
            val start = "${parts1[2]}-${parts1[1]}-${parts1[0]}"
            val end = "${parts2[2]}-${parts2[1]}-${parts2[0]}"
            
            end > start
        } catch (e: Exception) {
            true // If parsing fails, allow the operation
        }
    }
    
    private fun showFollowerSelectionDialog() {
        val dialogBinding = DialogSelectFollowersBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)
        
        // Setup follower RecyclerView
        val followerAdapter = FollowerSelectionAdapter(
            onAddMember = { user ->
                addMember(user)
                dialog.dismiss()
            },
            selectedMembers = selectedMembers
        )
        
        dialogBinding.rvFollowers.apply {
            layoutManager = LinearLayoutManager(this@CreateTripActivity)
            adapter = followerAdapter
        }
        
        // Load followers
        lifecycleScope.launch {
            try {
                val followers = loadFollowers()
                if (followers.isEmpty()) {
                    dialogBinding.rvFollowers.visibility = View.GONE
                    dialogBinding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    dialogBinding.rvFollowers.visibility = View.VISIBLE
                    dialogBinding.layoutEmpty.visibility = View.GONE
                    followerAdapter.submitList(followers)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateTripActivity, "Error loading followers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        // Close button
        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private suspend fun loadFollowers(): List<User> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return emptyList()
            android.util.Log.d("CreateTripActivity", "Current userId: $currentUserId")
            
            // Get followers from backend API
            val result = tripRepository.getFollowers(currentUserId)
            
            result.onSuccess { followers ->
                android.util.Log.d("CreateTripActivity", "API SUCCESS: Received ${followers.size} followers")
                followers.forEachIndexed { index, user ->
                    android.util.Log.d("CreateTripActivity", "  [$index] ${user.firstName} ${user.lastName} (${user.id})")
                }
                
                // Filter out already selected members
                val filtered = followers.filter { user ->
                    !selectedMembers.any { it.id == user.id }
                }
                android.util.Log.d("CreateTripActivity", "After filtering: ${filtered.size} followers")
                android.util.Log.d("CreateTripActivity", "=== loadFollowers END ===")
                return filtered
            }.onFailure { exception ->
                android.util.Log.e("CreateTripActivity", "API FAILED: ${exception.message}", exception)
                return emptyList()
            }
            
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("CreateTripActivity", "Error loading followers: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun addMember(user: User) {
        if (!selectedMembers.any { it.id == user.id }) {
            selectedMembers.add(user)
            // Create new list to trigger DiffUtil
            memberAdapter.submitList(ArrayList(selectedMembers))
            android.util.Log.d("CreateTripActivity", "Added member: ${user.firstName}, total: ${selectedMembers.size}")
            Toast.makeText(this, "${user.firstName} added as travel buddy", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeMember(user: User) {
        android.util.Log.d("CreateTripActivity", "Removing member: ${user.firstName} (${user.id}), current size: ${selectedMembers.size}")
        
        val removed = selectedMembers.removeIf { it.id == user.id }
        
        if (removed) {
            android.util.Log.d("CreateTripActivity", "Member removed successfully, new size: ${selectedMembers.size}")
            // Create new list to trigger DiffUtil
            val newList = ArrayList(selectedMembers)
            memberAdapter.submitList(newList) {
                // Callback after list is submitted
                android.util.Log.d("CreateTripActivity", "Adapter updated with ${newList.size} members")
            }
            Toast.makeText(this, "${user.firstName} removed", Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.w("CreateTripActivity", "Failed to remove member: ${user.firstName}")
        }
    }
}
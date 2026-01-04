package com.datn.apptravel.ui.trip.detail.tripdetail

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.TopicSelection
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.model.TripTopic
import com.datn.apptravel.data.model.request.CreateTripRequest
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityTripDetailBinding
import com.datn.apptravel.databinding.DialogShareTripBinding
import com.datn.apptravel.databinding.DialogSelectFollowersBinding
import com.datn.apptravel.ui.trip.adapter.FollowerSelectionAdapter
import com.datn.apptravel.ui.trip.adapter.ScheduleDayAdapter
import com.datn.apptravel.ui.trip.adapter.TopicAdapter
import com.datn.apptravel.ui.trip.adapter.TripMemberAdapter
import com.datn.apptravel.ui.trip.adapter.TripMemberSmallAdapter
import com.datn.apptravel.ui.trip.TripsFragment
import com.datn.apptravel.ui.trip.list.PlanSelectionActivity
import com.datn.apptravel.ui.trip.map.TripMapActivity
import com.datn.apptravel.ui.trip.viewmodel.TripDetailViewModel
import com.datn.apptravel.utils.ApiConfig
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.datn.apptravel.data.model.UserInsight
import com.datn.apptravel.ui.trip.ai.AIInsightDialogFragment
import com.datn.apptravel.ui.trip.ai.AISuggestionPreviewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TripDetailActivity : AppCompatActivity() {

    private val viewModel: TripDetailViewModel by viewModel()
    private val tripRepository: TripRepository by inject()
    private val authRepository: AuthRepository by inject()
    private val auth: FirebaseAuth by inject()
    private var tripId: String? = null
    private var currentTrip: Trip? = null
    private lateinit var binding: ActivityTripDetailBinding
    private lateinit var scheduleDayAdapter: ScheduleDayAdapter
    private lateinit var memberAdapter: TripMemberSmallAdapter

    private var isOwner = false // Track if current user is trip owner
    private var isMember = false // Track if current user is trip member
    private var isReadOnly = false // Cho Discover

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide UI initially until ownership is verified
        binding.root.visibility = View.GONE

        isReadOnly = intent.getBooleanExtra("READ_ONLY", false) // cho Discover_service

        // Get trip ID from intent
        tripId = intent.getStringExtra(TripsFragment.Companion.EXTRA_TRIP_ID)
        
        Log.d("TripDetailActivity", "onCreate - tripId: $tripId, isReadOnly: $isReadOnly")

        setupUI()
        setupObservers()

        // Load trip details
        loadTripData()
    }

    override fun onResume() {
        super.onResume()

        // Reload trip data when returning from other screens
        loadTripData()
    }

    private fun loadTripData() {
        tripId?.let {
            Log.d("TripDetailActivity", "loadTripData - Loading trip: $it")
            viewModel.getTripDetails(it)
        } ?: run {
            Log.d("TripDetailActivity", "loadTripData - No trip ID")
            // If no trip ID, show empty state
            binding.emptyPlansContainer.visibility = View.VISIBLE
            binding.recyclerViewSchedule.visibility = View.GONE
        }
    }

    private fun setupUI() {
        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup menu button for opening menu
        binding.btnMenu.setOnClickListener {
            showTripMenu()
        }

        // Setup add new plan button - will be updated based on trip status
        binding.btnAddNewPlan.setOnClickListener {
            handlePlanButtonClick()
        }

        // Setup share button
        binding.btnShareTrip.setOnClickListener {
            showShareDialog()
        }

        binding.btnAiSuggest.setOnClickListener {
            showAIInsightDialog()
        }

        //Cho Discover_service
        if (isReadOnly) {
            binding.btnAddNewPlan.visibility = View.GONE
            binding.btnShareTrip.visibility = View.GONE
        }

        // Setup schedule RecyclerView
        setupRecyclerView()
    }

    private fun showAIInsightDialog() {
        val dialog = AIInsightDialogFragment.newInstance()

        dialog.setOnResultListener { insight ->
            // User đã chọn answer hoặc skip
            navigateToAISuggestions(insight)
        }

        dialog.show(supportFragmentManager, "AIInsightDialog")
    }

//    Navigate to AI Suggestion Preview Activity

    private fun navigateToAISuggestions(userInsight: UserInsight?) {
        val trip = currentTrip ?: run {
            Toast.makeText(this, "Không tìm thấy thông tin chuyến đi", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AISuggestionPreviewActivity::class.java)
        intent.putExtra("tripId", tripId)
        intent.putExtra("startDate", trip.startDate)
        intent.putExtra("endDate", trip.endDate)
        intent.putExtra("userInsight", userInsight)

        startActivity(intent)
    }

    private fun setupRecyclerView() {
        scheduleDayAdapter = ScheduleDayAdapter(emptyList(), isReadOnly) // thêm isReadOnly Cho Discover_service

        binding.recyclerViewSchedule.apply {
            adapter = scheduleDayAdapter
            layoutManager = LinearLayoutManager(this@TripDetailActivity)
        }
        
        // Setup member RecyclerView
        memberAdapter = TripMemberSmallAdapter()
        binding.rvTripMembers.apply {
            adapter = memberAdapter
            layoutManager = LinearLayoutManager(this@TripDetailActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupObservers() {
        Log.d("TripDetailActivity", "setupObservers - Setting up observers")
        
        // Observe trip details
        viewModel.tripDetails.observe(this) { trip ->
            Log.d("TripDetailActivity", "Observer triggered - trip: ${trip?.id}, userId: ${trip?.userId}")
            Log.d("TripDetailActivity", "Full trip object: $trip")
            currentTrip = trip
            
            // Check if current user is the trip owner
            if (trip != null) {
                Log.d("TripDetailActivity", "Checking ownership - trip is not null")
                lifecycleScope.launch {
                    val user = authRepository.currentUser.first()
                    Log.d("TripDetailActivity", "Current user ID: ${user?.id}")
                    Log.d("TripDetailActivity", "Trip user ID: ${trip.userId}")

                    if (user != null) {
                        // Check if user is owner
                        isOwner = user.id == trip.userId

                        // Check if user is member
                        Log.d("TripDetailActivity", "Trip members list: ${trip.members}")
                        Log.d("TripDetailActivity", "Trip members count: ${trip.members?.size}")
                        isMember = trip.members?.any { member ->
                            Log.d("TripDetailActivity", "Checking member: ${member.id} vs user: ${user.id}")
                            member.id == user.id
                        } == true

                        Log.d("TripDetailActivity", "Is owner: $isOwner")
                        Log.d("TripDetailActivity", "Is member: $isMember")
                        Log.d("TripDetailActivity", "isReadOnly: $isReadOnly")

                        // If user is neither owner nor member and not in read-only mode
                        // Redirect to TripMapActivity
                        if (!isOwner && !isMember && !isReadOnly) {
                            Log.d("TripDetailActivity", "User is neither owner nor member, redirecting to map view")
                            val intent = Intent(this@TripDetailActivity, TripMapActivity::class.java)
                            intent.putExtra("tripId", tripId)
                            intent.putExtra("tripTitle", trip.title ?: "Trip")
                            intent.putExtra("tripUserId", trip.userId)
                            startActivity(intent)
                            finish()
                            return@launch
                        }

                        // Show the UI
                        binding.root.visibility = View.VISIBLE

                        // Update UI based on ownership/membership
                        updateUIBasedOnPermissions()
                        updateUI(trip)
                    } else {
                        // No user logged in, redirect to map
                        if (!isReadOnly) {
                            val intent = Intent(this@TripDetailActivity, TripMapActivity::class.java)
                            intent.putExtra("tripId", tripId)
                            intent.putExtra("tripTitle", trip.title ?: "Trip")
                            intent.putExtra("tripUserId", trip.userId)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            } else {
                // Show UI if trip is null to display empty state
                binding.root.visibility = View.VISIBLE
                updateUI(trip)
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Observe schedule days
        viewModel.scheduleDays.observe(this) { scheduleDays ->
            if (scheduleDays.isNotEmpty()) {
                scheduleDayAdapter.updateScheduleDays(scheduleDays)
                binding.emptyPlansContainer.visibility = View.GONE
                binding.recyclerViewSchedule.visibility = View.VISIBLE
            } else {
                binding.emptyPlansContainer.visibility = View.VISIBLE
                binding.recyclerViewSchedule.visibility = View.GONE
            }
        }
    }

    private fun updateUI(trip: Trip?) {
        // Get views from included layout using root view
        val tvTripName = findViewById<TextView>(R.id.tvTripName)
        val tvTripStartDate = findViewById<TextView>(R.id.tvTripStartDate)
        val tvTripEndDate = findViewById<TextView>(R.id.tvTripEndDate)
        val ivTripImage = findViewById<ImageView>(R.id.ivTripImage)

        if (trip == null) {
            tvTripName?.text = ""
            tvTripStartDate?.text = ""
            tvTripEndDate?.text = ""
            return
        }

        // Update button text based on trip status
        updatePlanButtonBasedOnTripStatus(trip)

        // Set trip details from API
        tvTripName?.text = trip.title ?: "Untitled Trip"

        // Format dates from yyyy-MM-dd to dd-MM-yyyy
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        val formattedStartDate = try {
            val date = LocalDate.parse(trip.startDate, inputFormatter)
            date.format(outputFormatter)
        } catch (e: Exception) {
            trip.startDate
        }

        val formattedEndDate = try {
            val date = LocalDate.parse(trip.endDate, inputFormatter)
            date.format(outputFormatter)
        } catch (e: Exception) {
            trip.endDate
        }

        val startDate = "Start Date: $formattedStartDate"
        val endDate = "End Date: $formattedEndDate"
        tvTripStartDate?.text = startDate
        tvTripEndDate?.text = endDate

        // Display members based on current user role
        lifecycleScope.launch {
            val currentUser = authRepository.currentUser.first()
            val currentUserId = currentUser?.id

            if (currentUserId != null && trip.userId != null) {
                val displayList = mutableListOf<com.datn.apptravel.data.model.User>()

                if (isOwner) {
                    // Owner: Hiển thị tất cả members (không bao gồm bản thân)
                    trip.members?.forEach { member ->
                        if (member.id != currentUserId) {
                            displayList.add(member)
                        }
                    }
                    android.util.Log.d("TripDetailActivity", "Owner mode: Displaying ${displayList.size} members (excluding self)")
                } else if (isMember) {
                    // Member: Hiển thị owner đầu tiên, sau đó các members khác (không bao gồm bản thân)
                    // Fetch owner info
                    val ownerResult = tripRepository.getUserById(trip.userId)
                    ownerResult.onSuccess { owner ->
                        displayList.add(owner)
                    }

                    // Add other members (excluding self)
                    trip.members?.forEach { member ->
                        if (member.id != currentUserId) {
                            displayList.add(member)
                        }
                    }
                    android.util.Log.d("TripDetailActivity", "Member mode: Displaying owner + ${displayList.size - 1} other members (excluding self)")
                }

                // Update UI
                if (displayList.isNotEmpty()) {
                    binding.layoutTravelBuddies.visibility = View.VISIBLE
                    memberAdapter.submitList(displayList)
                    android.util.Log.d("TripDetailActivity", "Displaying ${displayList.size} users in travel buddies")
                } else {
                    binding.layoutTravelBuddies.visibility = View.GONE
                    android.util.Log.d("TripDetailActivity", "No travel buddies to display")
                }
            } else {
                binding.layoutTravelBuddies.visibility = View.GONE
            }
        }

        // Load cover photo if available
        val imageUrl = ApiConfig.getImageUrl(trip.coverPhoto)
        if (imageUrl != null) {
            ivTripImage?.let {
                Glide.with(this@TripDetailActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_a)
                    .error(R.drawable.bg_a)
                    .centerCrop()
                    .into(it)
            }
        } else {
            // Set default image if no cover photo
            ivTripImage?.setImageResource(R.drawable.bg_a)
        }
    }

    /**
     * Update UI visibility based on user permissions
     */
    private fun updateUIBasedOnPermissions() {
        if (isOwner) {
            // Owner has full access to everything
            binding.btnShareTrip.visibility = View.VISIBLE
            binding.btnMenu.visibility = View.VISIBLE
            binding.btnAddNewPlan.visibility = View.VISIBLE
            Log.d("TripDetailActivity", "Owner mode: Full access")
        } else if (isMember) {
            // Member can only add plans and view map
            // Hide share button, edit and delete will be hidden in menu
            binding.btnShareTrip.visibility = View.GONE
            binding.btnMenu.visibility = View.VISIBLE
            binding.btnAddNewPlan.visibility = View.VISIBLE
            Log.d("TripDetailActivity", "Member mode: Limited access (add plan, view map only)")
        } else if (isReadOnly) {
            // Read-only mode (from Discover) - can only view
            binding.btnAddNewPlan.visibility = View.GONE
            binding.btnShareTrip.visibility = View.GONE
            binding.btnMenu.visibility = View.VISIBLE
            Log.d("TripDetailActivity", "Read-only mode: View only")
        }
    }

    private fun showTripMenu() {
        val popupMenu = PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.trip_detail_menu, popupMenu.menu)

        // Hide "View Map" menu item if trip has ended
        val isTripEnded = isTripEnded()
        popupMenu.menu.findItem(R.id.action_view_map)?.isVisible = !isTripEnded

        // Only owner can edit and delete trip
        // Members and read-only users cannot see these options
        if (!isOwner) {
            popupMenu.menu.findItem(R.id.action_edit_trip)?.isVisible = false
            popupMenu.menu.findItem(R.id.action_delete_trip)?.isVisible = false
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view_map -> {
                    // Open map view
                    navigateToMapView()
                    true
                }
                R.id.action_edit_trip -> {
                    // Open trip edit
                    currentTrip?.let { trip ->
                        val intent = Intent(this, com.datn.apptravel.ui.trip.CreateTripActivity::class.java)
                        intent.putExtra(com.datn.apptravel.ui.trip.CreateTripActivity.EXTRA_TRIP_ID, trip.id.toString())
                        intent.putExtra(com.datn.apptravel.ui.trip.CreateTripActivity.EXTRA_TRIP_TITLE, trip.title)
                        intent.putExtra(com.datn.apptravel.ui.trip.CreateTripActivity.EXTRA_START_DATE, trip.startDate)
                        intent.putExtra(com.datn.apptravel.ui.trip.CreateTripActivity.EXTRA_END_DATE, trip.endDate)
                        intent.putExtra(com.datn.apptravel.ui.trip.CreateTripActivity.EXTRA_COVER_PHOTO, trip.coverPhoto)
                        startActivity(intent)
                    } ?: run {
                        Toast.makeText(this, "Trip data not available", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_delete_trip -> {
                    // Show delete confirmation
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete this trip?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTrip() {
        val tripIdToDelete = tripId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val result = tripRepository.deleteTrip(tripIdToDelete)

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(this@TripDetailActivity, "Trip deleted successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Notify previous screen to refresh
                    finish()
                } else {
                    Toast.makeText(
                        this@TripDetailActivity,
                        "Failed to delete trip: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showShareDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogShareTripBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val trip = currentTrip
        val hasBeenShared = trip?.sharedAt != null
        var currentPrivacy = trip?.isPublic ?: "none"
        val existingTags = trip?.tags?.split(",")?.map { it.trim() } ?: emptyList()
        val existingContent = trip?.content ?: ""
        
        // Track selected shared users
        val selectedSharedUserIds = mutableSetOf<String>()
        trip?.sharedWithUsers?.mapNotNull { it.id }?.let { selectedSharedUserIds.addAll(it) }

        // Pre-fill feelings if trip already has content
        if (existingContent.isNotEmpty()) {
            dialogBinding.etFeelings.setText(existingContent)
        }

        // Setup privacy selector text
        fun updatePrivacyText(privacy: String) {
            val displayText = when (privacy) {
                "public" -> "Public"
                "follower" -> "Follower"
                else -> "Private"
            }
            dialogBinding.tvPrivacyValue.text = displayText
        }
        
        // Update shared users section visibility
        fun updateSharedUsersVisibility() {
            if (currentPrivacy == "follower") {
                dialogBinding.layoutSharedUsers.visibility = android.view.View.VISIBLE
            } else {
                dialogBinding.layoutSharedUsers.visibility = android.view.View.GONE
            }
        }

        // Initialize privacy display
        updatePrivacyText(currentPrivacy)
        updateSharedUsersVisibility()

        // Setup privacy selector click listener
        dialogBinding.layoutPrivacySelector.setOnClickListener {
            val popupMenu = PopupMenu(this, dialogBinding.layoutPrivacySelector)
            popupMenu.menu.add(0, 0, 0, "Private")
            popupMenu.menu.add(0, 1, 1, "Public")
            popupMenu.menu.add(0, 2, 2, "Follower")

            popupMenu.setOnMenuItemClickListener { menuItem ->
                currentPrivacy = when (menuItem.itemId) {
                    1 -> "public"
                    2 -> "follower"
                    else -> "none"
                }
                updatePrivacyText(currentPrivacy)
                updateSharedUsersVisibility()
                true
            }
            popupMenu.show()
        }
        
        // Track selected shared users list
        val selectedSharedUsers = mutableListOf<com.datn.apptravel.data.model.User>()
        
        // Setup shared users RecyclerView with member adapter
        lateinit var sharedUserAdapter: TripMemberAdapter
        sharedUserAdapter = TripMemberAdapter(
            onRemoveMember = { user ->
                selectedSharedUsers.removeIf { it.id == user.id }
                selectedSharedUserIds.remove(user.id ?: "")
                // Create new list to trigger DiffUtil
                sharedUserAdapter.submitList(ArrayList(selectedSharedUsers))
                Log.d("TripDetail", "Removed user, remaining: ${selectedSharedUsers.size}")
            }
        )
        
        dialogBinding.rvSharedUsers.apply {
            adapter = sharedUserAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@TripDetailActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            setHasFixedSize(false) // Allow height changes
            visibility = android.view.View.VISIBLE
        }
        
        Log.d("TripDetail", "RecyclerView setup complete")
        
        // Load existing shared users if available
        trip?.sharedWithUsers?.let { existingUsers ->
            selectedSharedUsers.addAll(existingUsers)
            selectedSharedUserIds.addAll(existingUsers.mapNotNull { it.id })
            Log.d("TripDetail", "Loading existing ${existingUsers.size} shared users")
            sharedUserAdapter.submitList(ArrayList(selectedSharedUsers)) {
                Log.d("TripDetail", "Existing users loaded, adapter count: ${sharedUserAdapter.itemCount}")
            }
        }
        
        // Handle select shared users button click
        dialogBinding.btnSelectSharedUsers.setOnClickListener {
            Log.d("TripDetail", "Select shared users button clicked")
            showFollowerSelectionDialog { selectedUsers ->
                Log.d("TripDetail", "Received ${selectedUsers.size} users from dialog")
                // Add only new users that aren't already selected
                selectedUsers.forEach { user ->
                    Log.d("TripDetail", "Processing user: ${user.firstName} ${user.lastName}, id: ${user.id}")
                    if (!selectedSharedUsers.any { it.id == user.id }) {
                        selectedSharedUsers.add(user)
                        selectedSharedUserIds.add(user.id ?: "")
                        Log.d("TripDetail", "Added user: ${user.firstName} ${user.lastName}")
                    } else {
                        Log.d("TripDetail", "User already in list, skipping")
                    }
                }
                
                // Update RecyclerView with new list
                val newList = ArrayList(selectedSharedUsers)
                Log.d("TripDetail", "Submitting list with ${newList.size} users to adapter")
                newList.forEachIndexed { index, user ->
                    Log.d("TripDetail", "  [$index] ${user.firstName} ${user.lastName}")
                }
                sharedUserAdapter.submitList(newList) {
                    Log.d("TripDetail", "List submitted successfully, adapter count: ${sharedUserAdapter.itemCount}")
                }
            }
        }

        // Create topic list with all available topics and pre-select existing ones
        val topicSelections = listOf(
            TopicSelection(TripTopic.CUISINE, existingTags.contains(TripTopic.CUISINE.topicName)),
            TopicSelection(TripTopic.DESTINATION, existingTags.contains(TripTopic.DESTINATION.topicName)),
            TopicSelection(TripTopic.ADVENTURE, existingTags.contains(TripTopic.ADVENTURE.topicName)),
            TopicSelection(TripTopic.RESORT, existingTags.contains(TripTopic.RESORT.topicName))
        )

        // Update UI based on whether trip has been shared
        if (hasBeenShared) {
            dialogBinding.btnDone.text = "Save"
            dialogBinding.tvShareTitle.text = "Edit Post"
        } else {
            dialogBinding.btnDone.text = "Share"
            dialogBinding.tvShareTitle.text = "Share Trip"
        }

        // Setup topics RecyclerView
        val topicAdapter = TopicAdapter(topicSelections) { topic, isChecked ->
            // Handle topic selection
            Log.d("TripDetail", "Topic ${topic.topic.topicName} selected: $isChecked")
        }

        dialogBinding.rvTopics.apply {
            adapter = topicAdapter
            layoutManager = GridLayoutManager(this@TripDetailActivity, 2)
            setHasFixedSize(true)
        }

        // Handle close button
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Handle done button
        dialogBinding.btnDone.setOnClickListener {
            val feelings = dialogBinding.etFeelings.text.toString().trim()
            val selectedTopics = topicSelections.filter { it.isSelected }

            // Validate feelings input
            if (feelings.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please share your thoughts",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validate topic selection
            if (selectedTopics.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please select at least one topic",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (currentTrip == null) {
                Toast.makeText(
                    this,
                    "Trip information not found",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Prepare data for sharing
            val tags = selectedTopics.joinToString(",") { it.topic.topicName }

            // Use currentPrivacy variable from the privacy selector
            val selectedPrivacy = currentPrivacy

            // Determine sharedAt: set current time if first share, keep existing otherwise
            val sharedAtValue = if (trip?.sharedAt != null) {
                trip.sharedAt // Keep existing timestamp
            } else if (selectedPrivacy != "none") {
                java.time.LocalDateTime.now().toString() // First time sharing
            } else {
                null // Not sharing
            }

            // Prepare shared user list (empty list if none selected means share with all followers)
            val sharedUsersList = if (currentPrivacy == "follower") {
                selectedSharedUsers.toList()
            } else {
                null
            }

            // Call API to update trip
            shareTrip(feelings, tags, selectedPrivacy, sharedAtValue, sharedUsersList, dialog)
        }

        dialog.show()
    }

    private fun shareTrip(content: String, tags: String, isPublic: String, sharedAt: String?, sharedWithUsers: List<com.datn.apptravel.data.model.User>?, dialog: Dialog) {
        currentTrip?.let { trip ->
            val updateRequest = CreateTripRequest(
                userId = trip.userId,
                title = trip.title,
                startDate = trip.startDate,
                endDate = trip.endDate,
                isPublic = isPublic,
                coverPhoto = trip.coverPhoto,
                content = content,
                tags = tags,
                members = trip.members,
                sharedWithUsers = sharedWithUsers,
                sharedAt = sharedAt
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = tripRepository.updateTrip(trip.id ?: "", updateRequest)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            var message=""
                            if(dialog.findViewById<TextView>(R.id.tvShareTitle)?.text?.toString().equals("Edit Post")){
                                message = when (isPublic) {
                                    "public" -> "Post updated successfully"
                                    "follower" -> "Post updated successfully"
                                    else -> "Post updated successfully"
                                }
                            }else{
                                message = when (isPublic) {
                                    "public" -> "Shared successfully"
                                    "follower" -> "Shared successfully"
                                    else -> "Post saved (not public)"
                                }
                            }

                            Toast.makeText(
                                this@TripDetailActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()

                            // Reload trip data
                            loadTripData()
                        } else {
                            Toast.makeText(
                                this@TripDetailActivity,
                                "Error: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TripDetailActivity,
                            "Connection error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun navigateToPlanSelection() {
        val intent = Intent(this, PlanSelectionActivity::class.java)
        intent.putExtra("tripId", tripId)
        startActivity(intent)
    }

    private fun navigateToMapView() {
        val intent = Intent(this, TripMapActivity::class.java)
        intent.putExtra("tripId", tripId)
        intent.putExtra("tripTitle", viewModel.tripDetails.value?.title ?: "Trip")
        intent.putExtra("tripUserId", currentTrip?.userId)
        startActivity(intent)
    }

    /**
     * Check if the trip has ended
     */
    private fun isTripEnded(): Boolean {
        val trip = currentTrip ?: return false
        return try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val endDate = LocalDate.parse(trip.endDate, dateFormatter)
            val today = LocalDate.now()
            endDate.isBefore(today)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update plan button text and icon based on trip status
     */
    private fun updatePlanButtonBasedOnTripStatus(trip: Trip) {
        if (isTripEnded()) {
            binding.btnAddNewPlan.text = "View Map"
        } else {
            binding.btnAddNewPlan.text = "Add new plan"
        }
    }

    /**
     * Handle plan button click - navigate to different screen based on trip status
     */
    private fun handlePlanButtonClick() {
        if (isTripEnded()) {
            navigateToMapView()
        } else {
            navigateToPlanSelection()
        }
    }
    
    /**
     * Show follower selection dialog for sharing with specific users
     */
    private fun showFollowerSelectionDialog(onUsersSelected: (List<com.datn.apptravel.data.model.User>) -> Unit) {
        val dialogBinding = DialogSelectFollowersBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)
        
        // Track selected users (single select mode for adding one at a time)
        val selectedUsers = mutableListOf<com.datn.apptravel.data.model.User>()
        
        // Setup follower RecyclerView with single-select adapter
        val followerAdapter = FollowerSelectionAdapter(
            onAddMember = { user ->
                selectedUsers.add(user)
                onUsersSelected(selectedUsers)
                dialog.dismiss()
            },
            selectedMembers = emptyList() // No pre-selection needed
        )
        
        dialogBinding.rvFollowers.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@TripDetailActivity)
            adapter = followerAdapter
        }
        
        // Load followers
        lifecycleScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val result = tripRepository.getFollowers(currentUserId)
                
                result.onSuccess { followers ->
                    if (followers.isEmpty()) {
                        dialogBinding.rvFollowers.visibility = android.view.View.GONE
                        dialogBinding.layoutEmpty.visibility = android.view.View.VISIBLE
                    } else {
                        dialogBinding.rvFollowers.visibility = android.view.View.VISIBLE
                        dialogBinding.layoutEmpty.visibility = android.view.View.GONE
                        followerAdapter.submitList(followers)
                    }
                }.onFailure {
                    Toast.makeText(this@TripDetailActivity, "Error loading followers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Close button
        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }


}
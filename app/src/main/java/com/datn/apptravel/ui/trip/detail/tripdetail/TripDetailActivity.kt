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
import com.datn.apptravel.data.model.AISuggestedPlan
import com.datn.apptravel.data.model.CityPlan
import com.datn.apptravel.data.model.User
import com.datn.apptravel.ui.trip.CreateTripActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.datn.apptravel.ui.trip.ai.AIDialogFragment
import com.datn.apptravel.ui.trip.ai.AIPlanPreviewDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TripDetailActivity : AppCompatActivity() {

    private val viewModel: TripDetailViewModel by viewModel()
    private val auth: FirebaseAuth by inject()
    private var tripId: String? = null
    private var currentTrip: Trip? = null
    private lateinit var binding: ActivityTripDetailBinding
    private lateinit var scheduleDayAdapter: ScheduleDayAdapter
    private lateinit var memberAdapter: TripMemberSmallAdapter

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
            showAISimpleInputDialog()
        }

        //Cho Discover_service
        if (isReadOnly) {
            binding.btnAddNewPlan.visibility = View.GONE
            binding.btnShareTrip.visibility = View.GONE
            binding.btnAiSuggest.visibility = View.GONE
        }

        // Setup schedule RecyclerView
        setupRecyclerView()
    }

    private fun showAISimpleInputDialog() {
        val trip = currentTrip
        if (trip == null) {
            Toast.makeText(this, "Không tìm thấy thông tin chuyến đi", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = AIDialogFragment.newInstance(trip.startDate, trip.endDate)

        dialog.setOnResultListener { cities ->
            // User đã nhập các thành phố và dates
            generateAIPlansForCities(cities)
        }

        dialog.show(supportFragmentManager, "AISimpleInputDialog")
    }

    private fun generateAIPlansForCities(cities: List<CityPlan>) {
        if (cities.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 thành phố", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        Toast.makeText(this, "Đang tạo lịch trình với AI...", Toast.LENGTH_LONG).show()

        // Call ViewModel to generate plans (without saving)
        viewModel.generateAIPlansForCities(cities)
    }

    private fun showAIPlanPreviewDialog(plans: List<AISuggestedPlan>) {
        val dialog = AIPlanPreviewDialogFragment.newInstance()
        
        dialog.setPlans(plans)
        dialog.setOnSaveListener { confirmedPlans ->
            // User confirmed, now save to Firestore
            val currentTripId = tripId ?: return@setOnSaveListener
            Toast.makeText(this, "Đang lưu ${confirmedPlans.size} kế hoạch...", Toast.LENGTH_SHORT).show()
            viewModel.saveAIPlans(this, currentTripId, confirmedPlans)
        }
        
        dialog.show(supportFragmentManager, "AIPlanPreviewDialog")
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
        
        // Set current user in ViewModel
        auth.currentUser?.uid?.let { userId ->
            viewModel.setCurrentUser(userId)
        }
        
        // Observe ownership/membership status from ViewModel
        viewModel.isOwner.observe(this) { isOwner ->
            // Check if user should be redirected to map view
            val isMember = viewModel.isMember.value ?: false
            if (!isOwner && !isMember && !isReadOnly && currentTrip != null) {
                Log.d("TripDetailActivity", "User is neither owner nor member, redirecting to map view")
                val intent = Intent(this, TripMapActivity::class.java)
                intent.putExtra("tripId", tripId)
                intent.putExtra("tripTitle", currentTrip?.title ?: "Trip")
                intent.putExtra("tripUserId", currentTrip?.userId)
                startActivity(intent)
                finish()
                return@observe
            }
            updateUIBasedOnPermissions()
        }
        
        viewModel.isMember.observe(this) { isMember ->
            // Check if user should be redirected to map view
            val isOwner = viewModel.isOwner.value ?: false
            if (!isOwner && !isMember && !isReadOnly && currentTrip != null) {
                Log.d("TripDetailActivity", "User is neither owner nor member, redirecting to map view")
                val intent = Intent(this, TripMapActivity::class.java)
                intent.putExtra("tripId", tripId)
                intent.putExtra("tripTitle", currentTrip?.title ?: "Trip")
                intent.putExtra("tripUserId", currentTrip?.userId)
                startActivity(intent)
                finish()
                return@observe
            }
            updateUIBasedOnPermissions()
        }
        
        // Observe trip details
        viewModel.tripDetails.observe(this) { trip ->
            Log.d("TripDetailActivity", "Observer triggered - trip: ${trip?.id}")
            currentTrip = trip
            binding.root.visibility = View.VISIBLE
            updateUI(trip)
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

        // Observe AI generation status
        viewModel.aiGenerationStatus.observe(this) { status ->
            when (status) {
                is TripDetailViewModel.AIGenerationStatus.Loading -> {
                    // Show loading (Toast already shown in generateAIPlans)
                }
                is TripDetailViewModel.AIGenerationStatus.Success -> {
                    Toast.makeText(
                        this,
                        "Đã tạo ${status.plansCreated} kế hoạch thành công!",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetAIGenerationStatus()
                }
                is TripDetailViewModel.AIGenerationStatus.Error -> {
                    Toast.makeText(
                        this,
                        "Lỗi: ${status.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetAIGenerationStatus()
                }
                is TripDetailViewModel.AIGenerationStatus.Idle -> {
                    // Do nothing
                }
            }
        }

        // Observe AI generated plans for preview
        viewModel.aiGeneratedPlans.observe(this) { plans ->
            if (plans != null && plans.isNotEmpty()) {
                // Show preview dialog with generated plans
                showAIPlanPreviewDialog(plans)
            }
        }
        
        // Observe display members
        viewModel.displayMembers.observe(this) { members ->
            if (members.isNotEmpty()) {
                binding.layoutTravelBuddies.visibility = View.VISIBLE
                memberAdapter.submitList(members)
                Log.d("TripDetailActivity", "Displaying ${members.size} users in travel buddies")
            } else {
                binding.layoutTravelBuddies.visibility = View.GONE
            }
        }
        
        // Observe plan button text
        viewModel.planButtonText.observe(this) { text ->
            binding.btnAddNewPlan.text = text
        }
        
        // Observe trip ended status
        viewModel.isTripEnded.observe(this) { ended ->
            // Update menu visibility if needed
        }
        
        // Observe can view map
        viewModel.canViewMap.observe(this) { canView ->
            // Will be used in menu
        }
        
        // Observe show share button
        viewModel.showShareButton.observe(this) { show ->
            binding.btnShareTrip.visibility = if (show) View.VISIBLE else View.GONE
        }
        
        // Observe delete success
        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Trip deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        // Observe share success
        viewModel.shareSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Trip shared successfully", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe followers
        viewModel.followers.observe(this) { followers ->
            // Followers list will be used in dialog
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

    private fun updateUIBasedOnPermissions() {
        val isOwner = viewModel.isOwner.value ?: false
        val isMember = viewModel.isMember.value ?: false
        
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
        val canViewMap = viewModel.canViewMap.value ?: true
        popupMenu.menu.findItem(R.id.action_view_map)?.isVisible = canViewMap

        // Only owner can edit and delete trip
        // Members and read-only users cannot see these options
        val isOwner = viewModel.isOwner.value ?: false
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
                        val intent = Intent(this, CreateTripActivity::class.java)
                        intent.putExtra(CreateTripActivity.EXTRA_TRIP_ID, trip.id.toString())
                        intent.putExtra(CreateTripActivity.EXTRA_TRIP_TITLE, trip.title)
                        intent.putExtra(CreateTripActivity.EXTRA_START_DATE, trip.startDate)
                        intent.putExtra(CreateTripActivity.EXTRA_END_DATE, trip.endDate)
                        intent.putExtra(CreateTripActivity.EXTRA_COVER_PHOTO, trip.coverPhoto)
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
        viewModel.deleteTrip(tripIdToDelete)
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
        val selectedSharedUsers = mutableListOf<User>()
        
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

    private fun shareTrip(content: String, tags: String, isPublic: String, sharedAt: String?, sharedWithUsers: List<User>?, dialog: Dialog) {
        currentTrip?.let { trip ->
            tripId?.let { id ->
                viewModel.shareTrip(
                    tripId = id,
                    userId = trip.userId,
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    coverPhoto = trip.coverPhoto,
                    content = content,
                    tags = tags,
                    isPublic = isPublic,
                    sharedAt = sharedAt,
                    members = trip.members,
                    sharedWithUsers = sharedWithUsers
                )
                
                // Show appropriate message
                val hasBeenShared = trip.sharedAt != null
                val message = if (hasBeenShared) {
                    "Post updated successfully"
                } else {
                    when (isPublic) {
                        "public" -> "Shared successfully"
                        "follower" -> "Shared successfully"
                        else -> "Post saved (not public)"
                    }
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                dialog.dismiss()
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

    private fun handlePlanButtonClick() {
        val isTripEnded = viewModel.isTripEnded.value ?: false
        if (isTripEnded) {
            navigateToMapView()
        } else {
            navigateToPlanSelection()
        }
    }

    private fun showFollowerSelectionDialog(onUsersSelected: (List<User>) -> Unit) {
        val dialogBinding = DialogSelectFollowersBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)
        
        // Track selected users (single select mode for adding one at a time)
        val selectedUsers = mutableListOf<User>()
        
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
        
        // Load followers from ViewModel
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            viewModel.loadFollowers(currentUserId)
        }
        
        // Observe followers and update adapter
        viewModel.followers.observe(this) { followers ->
            if (followers.isEmpty()) {
                dialogBinding.rvFollowers.visibility = android.view.View.GONE
                dialogBinding.layoutEmpty.visibility = android.view.View.VISIBLE
            } else {
                dialogBinding.rvFollowers.visibility = android.view.View.VISIBLE
                dialogBinding.layoutEmpty.visibility = android.view.View.GONE
                followerAdapter.submitList(followers)
            }
        }
        
        // Close button
        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }


}
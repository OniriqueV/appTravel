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
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityTripDetailBinding
import com.datn.apptravel.databinding.DialogShareTripBinding
import com.datn.apptravel.ui.trip.adapter.ScheduleDayAdapter
import com.datn.apptravel.ui.trip.adapter.TopicAdapter
import com.datn.apptravel.ui.trip.TripsFragment
import com.datn.apptravel.ui.trip.list.PlanSelectionActivity
import com.datn.apptravel.ui.trip.map.TripMapActivity
import com.datn.apptravel.ui.trip.viewmodel.TripDetailViewModel
import com.datn.apptravel.utils.ApiConfig
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
    private val tripRepository: TripRepository by inject()
    private var tripId: String? = null
    private var currentTrip: Trip? = null
    private lateinit var binding: ActivityTripDetailBinding
    private lateinit var scheduleDayAdapter: ScheduleDayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get trip ID from intent
        tripId = intent.getStringExtra(TripsFragment.Companion.EXTRA_TRIP_ID)

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
            viewModel.getTripDetails(it)
        } ?: run {
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

        // Setup schedule RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        scheduleDayAdapter = ScheduleDayAdapter(emptyList())
        binding.recyclerViewSchedule.apply {
            adapter = scheduleDayAdapter
            layoutManager = LinearLayoutManager(this@TripDetailActivity)
        }
    }

    private fun setupObservers() {
        // Observe trip details
        viewModel.tripDetails.observe(this) { trip ->
            currentTrip = trip
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

    private fun showTripMenu() {
        val popupMenu = PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.trip_detail_menu, popupMenu.menu)

        // Hide "View Map" menu item if trip has ended
        val isTripEnded = isTripEnded()
        popupMenu.menu.findItem(R.id.action_view_map)?.isVisible = !isTripEnded

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

        // Initialize privacy display
        updatePrivacyText(currentPrivacy)

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
                true
            }
            popupMenu.show()
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
            dialogBinding.btnDone.text = "Lưu"
            dialogBinding.tvShareTitle.text = "Chỉnh sửa bài viết"
        } else {
            dialogBinding.btnDone.text = "Chia sẻ"
            dialogBinding.tvShareTitle.text = "Chia sẻ chuyến đi"
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
                    "Vui lòng nhập cảm nhận của bạn",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validate topic selection
            if (selectedTopics.isEmpty()) {
                Toast.makeText(
                    this,
                    "Vui lòng chọn ít nhất một chủ đề",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (currentTrip == null) {
                Toast.makeText(
                    this,
                    "Không tìm thấy thông tin chuyến đi",
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

            // Call API to update trip
            shareTrip(feelings, tags, selectedPrivacy, sharedAtValue, dialog)
        }

        dialog.show()
    }

    private fun shareTrip(content: String, tags: String, isPublic: String, sharedAt: String?, dialog: Dialog) {
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
                sharedAt = sharedAt
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = tripRepository.updateTrip(trip.id ?: "", updateRequest)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            var message=""
                            if(dialog.findViewById<TextView>(R.id.tvShareTitle)?.text?.toString().equals("Chỉnh sửa bài viết")){
                                message = when (isPublic) {
                                    "public" -> "Cập nhật bài viết thành công"
                                    "follower" -> "Cập nhật bài viết thành công"
                                    else -> "Cập nhật bài viết thành công"
                                }
                            }else{
                                message = when (isPublic) {
                                    "public" -> "Đã chia sẻ thành công"
                                    "follower" -> "Đã chia sẻ thành công"
                                    else -> "Đã lưu bài viết (không công khai)"
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
                                "Lỗi: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TripDetailActivity,
                            "Lỗi kết nối: ${e.message}",
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


}
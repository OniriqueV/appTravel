package com.datn.apptravel.ui.trip.detail.tripdetail

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

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

        // Setup add new plan button
        binding.btnAddNewPlan.setOnClickListener {
            navigateToPlanSelection()
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
        if (trip == null) {
            binding.tvTripName.text = ""
            binding.tvTripDate.text = ""
            return
        }

        // Set trip details from API
        binding.apply {
            tvTripName.text = trip.title ?: "Untitled Trip"

            // Format dates
            val startDate = trip.startDate?.toString() ?: "N/A"
            val endDate = trip.endDate?.toString() ?: "N/A"
            tvTripDate.text = "$startDate - $endDate"

            // Load cover photo if available
            if (!trip.coverPhoto.isNullOrEmpty()) {
                // TODO: Load image with Glide
                // Glide.with(this@TripDetailActivity).load(trip.coverPhoto).into(ivTripImage)
            }
        }
    }

    private fun showTripMenu() {
        val popupMenu = PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.trip_detail_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view_map -> {
                    // Open map view
                    navigateToMapView()
                    true
                }
                R.id.action_edit_trip -> {
                    // Open trip edit
                    Toast.makeText(this, "Edit trip coming soon", Toast.LENGTH_SHORT).show()
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
                // Delete the trip
                Toast.makeText(this, "Trip deleted!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        // Create topic list with all available topics
        val topicSelections = listOf(
            TopicSelection(TripTopic.CUISINE, false),
            TopicSelection(TripTopic.DESTINATION, false),
            TopicSelection(TripTopic.ADVENTURE, false),
            TopicSelection(TripTopic.RESORT, false)
        )

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

            if (selectedTopics.isEmpty()) {
                Toast.makeText(
                    this,
                    "Vui lòng chọn ít nhất một chủ đề",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (currentTrip == null) {
                Toast.makeText(
                    this,
                    "Không tìm thấy thông tin chuyến đi",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Prepare data for sharing
                val tags = selectedTopics.joinToString(",") { it.topic.topicName }

                // Call API to update trip
                shareTrip(feelings, tags, dialog)
            }
        }

        dialog.show()
    }

    private fun shareTrip(content: String, tags: String, dialog: Dialog) {
        currentTrip?.let { trip ->
            val updateRequest = CreateTripRequest(
                userId = trip.userId,
                title = trip.title,
                startDate = trip.startDate,
                endDate = trip.endDate,
                isPublic = true,  // Set to public when sharing
                coverPhoto = trip.coverPhoto,
                content = content.ifEmpty { trip.content },  // Use new feelings or keep old
                tags = tags  // New selected topics
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = tripRepository.updateTrip(trip.id ?: "", updateRequest)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@TripDetailActivity,
                                "Đã chia sẻ chuyến đi với chủ đề: $tags",
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
        startActivity(intent)
    }
}
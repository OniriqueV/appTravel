package com.datn.apptravel.ui.trip.detail

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.R
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityPlanDetailBinding
import com.datn.apptravel.ui.trip.adapter.PhotoCollectionAdapter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Locale

class PlanDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanDetailBinding
    private var plan: Plan? = null
    private var planId: String? = null
    private var tripId: String? = null
    private lateinit var photoAdapter: PhotoCollectionAdapter
    private val tripRepository: TripRepository by inject()
    private val selectedPhotos = mutableListOf<String>()

    // Multiple image picker launcher
    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uploadPhotos(uris)
        }
    }

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_TRIP_ID = "trip_id"
        const val EXTRA_PLAN_TITLE = "plan_title"
        const val EXTRA_PLAN_TYPE = "plan_type"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_EXPENSE = "expense"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_LIKES_COUNT = "likes_count"
        const val EXTRA_COMMENTS_COUNT = "comments_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadPlanData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnMenu.setOnClickListener {
            showPlanMenu()
        }

        // Setup photo collection RecyclerView
        photoAdapter = PhotoCollectionAdapter(mutableListOf()) {
            // Callback when Add Photo button is clicked
            multipleImagePickerLauncher.launch("image/*")
        }
        binding.rvPhotos.apply {
            layoutManager =
                LinearLayoutManager(this@PlanDetailActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photoAdapter
        }

        binding.bottomWriteComment.setOnClickListener {
            // Open comment input dialog
            showCommentDialog()
        }

        binding.ivLike.setOnClickListener {
            // TODO: Toggle like
            toggleLike()
        }
    }

    private fun loadPlanData() {
        // Get data from intent
        planId = intent.getStringExtra(EXTRA_PLAN_ID)
        tripId = intent.getStringExtra(EXTRA_TRIP_ID)

        Log.d("PlanDetailActivity", "Loaded from intent - tripId: $tripId, planId: $planId")

        val planTitle = intent.getStringExtra(EXTRA_PLAN_TITLE) ?: "Plan"
        val planTypeStr = intent.getStringExtra(EXTRA_PLAN_TYPE) ?: "OTHER"
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val endTime = intent.getStringExtra(EXTRA_END_TIME)
        val expense = intent.getDoubleExtra(EXTRA_EXPENSE, 0.0)
        val location = intent.getStringExtra(EXTRA_LOCATION)
        val likesCount = intent.getIntExtra(EXTRA_LIKES_COUNT, 0)
        val commentsCount = intent.getIntExtra(EXTRA_COMMENTS_COUNT, 0)

        val planType = try {
            PlanType.valueOf(planTypeStr)
        } catch (e: Exception) {
            PlanType.NONE
        }

        // Update UI based on plan type
        updateUIForPlanType(planType, planTitle)

        // Set title
        binding.tvPlanTitle.text = getPlanTypeDisplayName(planType)
        binding.tvPlanName.text = planTitle

        // Set icon
        binding.ivPlanIcon.setImageResource(getPlanTypeIcon(planType))

        // Set times
        if (startTime.isNotEmpty()) {
            displayTime(startTime, endTime, planType)
        }

        // Set expense - always show, display "0đ" if no expense
        if (expense > 0) {
            binding.tvExpense.text = formatExpense(expense)
        } else {
            binding.tvExpense.text = "0đ"
        }
        binding.tvExpense.visibility = View.VISIBLE

        // Set likes and comments count
        binding.tvLikesCount.text = likesCount.toString()
        binding.tvCommentsCount.text = commentsCount.toString()

        // Show comments section if there are comments
        if (commentsCount > 0) {
            binding.cardComments.visibility = View.VISIBLE
            // TODO: Load comments from API
        } else {
            binding.cardComments.visibility = View.GONE
        }

        // Load photos from API if planId and tripId are available
        if (planId != null && tripId != null) {
            loadPhotosFromApi(tripId!!, planId!!)
        }
    }

    private fun loadPhotosFromApi(tripId: String, planId: String) {
        lifecycleScope.launch {
            try {
                Log.d("PlanDetailActivity", "Loading photos from API - tripId: $tripId, planId: $planId")
                val result = tripRepository.getPlanById(tripId, planId)

                result.onSuccess { plan ->
                    Log.d("PlanDetailActivity", "Loaded plan with photos: ${plan.photos}")

                    // Update photos list
                    plan.photos?.let { photosList ->
                        if (photosList.isNotEmpty()) {
                            selectedPhotos.clear()
                            selectedPhotos.addAll(photosList)
                            photoAdapter.updatePhotos(selectedPhotos)
                            Log.d("PlanDetailActivity", "Updated adapter with ${photosList.size} photos")
                        }
                    }
                }.onFailure { exception ->
                    Log.e("PlanDetailActivity", "Failed to load photos", exception)
                    // Don't show error toast - just fail silently as photos are optional
                }
            } catch (e: Exception) {
                Log.e("PlanDetailActivity", "Error loading photos", e)
            }
        }
    }

    private fun updateUIForPlanType(planType: PlanType, planTitle: String) {
        // All plan types now use the same time display format
        // No need to toggle visibility for different layouts
    }

    private fun getPlanTypeDisplayName(planType: PlanType): String {
        return when (planType) {
            PlanType.LODGING -> "Lodging"
            PlanType.RESTAURANT -> "Restaurant"
            PlanType.FLIGHT -> "Flight"
            PlanType.CAR_RENTAL -> "Car Rental"
            PlanType.TRAIN -> "Train"
            PlanType.BOAT -> "Boat"
            PlanType.TOUR -> "Tour"
            else -> "Activity"
        }
    }

    private fun getPlanTypeIcon(planType: PlanType): Int {
        return when (planType) {
            PlanType.LODGING -> R.drawable.ic_lodging
            PlanType.RESTAURANT -> R.drawable.ic_restaurant
            PlanType.FLIGHT -> R.drawable.ic_flight
            PlanType.CAR_RENTAL -> R.drawable.ic_car
            PlanType.TRAIN -> R.drawable.ic_train
            PlanType.BOAT -> R.drawable.ic_boat
            PlanType.TOUR -> R.drawable.ic_location
            else -> R.drawable.ic_location
        }
    }

    private fun displayTime(startTime: String, endTime: String?, planType: PlanType) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
            val fullDateTimeFormat = SimpleDateFormat("EEEE dd MMMM - HH:mm", Locale.ENGLISH)

            val startDate = inputFormat.parse(startTime)

            if (startDate != null) {
                // Set date header (e.g., "30 September 2025")
                binding.tvDate.text = dateFormat.format(startDate)

                // Set full date time (e.g., "Tuesday 1 October - 09:00")
                binding.tvFullDateTime.text = fullDateTimeFormat.format(startDate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvDate.text = startTime
            binding.tvFullDateTime.text = startTime
        }
    }

    private fun formatExpense(expense: Double): String {
        return String.Companion.format(Locale.US, "%.0fđ", expense)
    }

    private fun showPlanMenu() {
        val popupMenu = PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.plan_detail_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_plan -> {
                    // TODO: Navigate to edit plan
                    Toast.makeText(this, "Edit plan coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete_plan -> {
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
            .setTitle("Delete Plan")
            .setMessage("Are you sure you want to delete this plan?")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: Delete plan from backend
                Toast.makeText(this, "Plan deleted!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCommentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_comment, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Make dialog background transparent to show custom rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etComment = dialogView.findViewById<EditText>(R.id.etComment)
        val tvCharCount = dialogView.findViewById<TextView>(R.id.tvCharCount)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnPost = dialogView.findViewById<MaterialButton>(R.id.btnPost)

        // Character counter
        etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                tvCharCount.text = "$length/500"
                btnPost.isEnabled = length > 0
            }
        })

        // Initial state
        btnPost.isEnabled = false

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnPost.setOnClickListener {
            val comment = etComment.text.toString().trim()
            if (comment.isNotEmpty()) {
                // TODO: Post comment to backend
                Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Update comments count
                val currentCount = binding.tvCommentsCount.text.toString().toIntOrNull() ?: 0
                binding.tvCommentsCount.text = (currentCount + 1).toString()
                binding.cardComments.visibility = View.VISIBLE
            }
        }

        dialog.show()

        // Auto focus on EditText and show keyboard
        etComment.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun toggleLike() {
        // TODO: Toggle like status in backend
        val currentLikes = binding.tvLikesCount.text.toString().toIntOrNull() ?: 0
        val isLiked = binding.ivLike.tag as? Boolean ?: false

        if (isLiked) {
            binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
            binding.tvLikesCount.text = (currentLikes - 1).toString()
            binding.ivLike.tag = false
        } else {
            binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
            binding.tvLikesCount.text = (currentLikes + 1).toString()
            binding.ivLike.tag = true
        }
    }

    private fun uploadPhotos(uris: List<Uri>) {
        Toast.makeText(this, "Uploading ${uris.size} photos...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val result = tripRepository.uploadImages(this@PlanDetailActivity, uris)

                result.onSuccess { fileNames ->
                    selectedPhotos.addAll(fileNames)
                    photoAdapter.updatePhotos(selectedPhotos)

                    // Save photo filenames to Plan in backend
                    if (planId != null && tripId != null) {
                        Log.d("PlanDetailActivity", "Saving photos - tripId: $tripId, planId: $planId, photos: $selectedPhotos")
                        savePlanPhotos(tripId!!, planId!!, selectedPhotos)
                    } else {
                        val errorMsg = "Cannot save photos - tripId: $tripId, planId: $planId"
                        Log.e("PlanDetailActivity", errorMsg)
                        Toast.makeText(
                            this@PlanDetailActivity,
                            "Uploaded ${fileNames.size} photos but cannot save to plan. Missing IDs.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.onFailure { exception ->
                    Log.e("PlanDetailActivity", "Upload failed", exception)
                    Toast.makeText(
                        this@PlanDetailActivity,
                        "Upload failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PlanDetailActivity", "Upload error", e)
                Toast.makeText(
                    this@PlanDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun savePlanPhotos(tripId: String, planId: String, photos: List<String>) {
        lifecycleScope.launch {
            try {
                Log.d("PlanDetailActivity", "Calling updatePlanPhotos API...")
                val result = tripRepository.updatePlanPhotos(tripId, planId, photos)

                result.onSuccess { updatedPlan ->
                    Log.d("PlanDetailActivity", "Successfully saved photos: ${updatedPlan.photos}")
                    Toast.makeText(
                        this@PlanDetailActivity,
                        "Saved ${photos.size} photos to plan!",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { exception ->
                    Log.e("PlanDetailActivity", "Failed to save photos", exception)
                    Toast.makeText(
                        this@PlanDetailActivity,
                        "Failed to save photos: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PlanDetailActivity", "Error saving photos", e)
                Toast.makeText(
                    this@PlanDetailActivity,
                    "Error saving photos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
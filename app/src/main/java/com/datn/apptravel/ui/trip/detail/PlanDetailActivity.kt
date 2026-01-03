package com.datn.apptravel.ui.trip.detail

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.R
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityPlanDetailBinding
import com.datn.apptravel.ui.trip.adapter.PhotoCollectionAdapter
import com.datn.apptravel.ui.trip.viewmodel.PlanDetailViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

class PlanDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanDetailBinding
    private val viewModel: PlanDetailViewModel by viewModel()
    private val tripRepository: TripRepository by inject()
    private var planId: String? = null
    private var tripId: String? = null
    private lateinit var photoAdapter: PhotoCollectionAdapter

    // Activity result launcher for edit plan
    private val editPlanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload plan data when returning from edit
            if (planId != null && tripId != null) {
                viewModel.loadPlanPhotos(tripId!!, planId!!)
                // Also reload plan data to update UI
                loadPlanDataFromViewModel()
            }
        }
    }

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
        observeViewModel()
        loadPlanData()
    }

    private fun observeViewModel() {
        // Observe plan data
        viewModel.plan.observe(this) { plan ->
            plan?.let {
                updateUIWithPlanData(it)
            }
        }

        // Observe photos
        viewModel.photos.observe(this) { photos ->
            photoAdapter.updatePhotos(photos)
        }

        // Observe likes count
        viewModel.likesCount.observe(this) { count ->
            binding.tvLikesCount.text = count.toString()
        }

        // Observe comments count
        viewModel.commentsCount.observe(this) { count ->
            binding.tvCommentsCount.text = count.toString()
            binding.cardComments.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        // Observe like status
        viewModel.isLiked.observe(this) { isLiked ->
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
            }
            binding.ivLike.tag = isLiked
        }

        // Observe upload success
        viewModel.uploadSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Photos uploaded successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadSuccess()
            }
        }

        // Observe comment posted
        viewModel.commentPosted.observe(this) { posted ->
            if (posted) {
                viewModel.resetCommentPosted()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // TODO: Show/hide loading indicator if needed
        }

        // Observe errors
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnMenu.setOnClickListener {
            showPlanMenu()
        }

        // Setup photo collection RecyclerView
        photoAdapter = PhotoCollectionAdapter(
            mutableListOf(),
            onAddPhotoClick = {
                // Callback when Add Photo button is clicked
                multipleImagePickerLauncher.launch("image/*")
            },
            onDeletePhotoClick = { photoFileName, photoIndex ->
                // Callback when Delete Photo button is clicked
                showDeletePhotoConfirmation(photoFileName, photoIndex)
            }
        )
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
            // Toggle like through ViewModel
            viewModel.toggleLike()
        }
    }

    private fun loadPlanData() {
        // Get data from intent
        planId = intent.getStringExtra(EXTRA_PLAN_ID)
        tripId = intent.getStringExtra(EXTRA_TRIP_ID)

        val planTitle = intent.getStringExtra(EXTRA_PLAN_TITLE) ?: "Plan"
        val planTypeStr = intent.getStringExtra(EXTRA_PLAN_TYPE) ?: "OTHER"
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val endTime = intent.getStringExtra(EXTRA_END_TIME)
        val expense = intent.getDoubleExtra(EXTRA_EXPENSE, 0.0)
        val location = intent.getStringExtra(EXTRA_LOCATION)
        val likesCount = intent.getIntExtra(EXTRA_LIKES_COUNT, 0)
        val commentsCount = intent.getIntExtra(EXTRA_COMMENTS_COUNT, 0)

        // Set initial counts in ViewModel
        viewModel.setInitialCounts(likesCount, commentsCount)

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

        // Load photos from API if planId and tripId are available
        if (planId != null && tripId != null) {
            viewModel.loadPlanPhotos(tripId!!, planId!!)
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
            PlanType.ACTIVITY -> "ACTIVITY"
            PlanType.THEATER -> "THEATER"
            PlanType.SHOPPING -> "THEATER"
            PlanType.CAMPING -> "CAMPING"
            PlanType.RELIGION -> "RELIGION"
            else -> "Activity"
        }
    }

    private fun getPlanTypeIcon(planType: PlanType): Int {
        return when (planType) {
            PlanType.LODGING -> R.drawable.ic_lodgingsss
            PlanType.RESTAURANT -> R.drawable.ic_restaurant
            PlanType.FLIGHT -> R.drawable.ic_flight
            PlanType.CAR_RENTAL -> R.drawable.ic_car
            PlanType.TRAIN -> R.drawable.ic_train
            PlanType.BOAT -> R.drawable.ic_boat
            PlanType.TOUR -> R.drawable.ic_toursss
            PlanType.ACTIVITY -> R.drawable.ic_attraction
            PlanType.THEATER, -> R.drawable.ic_theater
            PlanType.SHOPPING -> R.drawable.ic_shopping
            PlanType.CAMPING -> R.drawable.ic_location
            PlanType.RELIGION -> R.drawable.ic_religion
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

    private fun loadPlanDataFromViewModel() {
        // This is called after edit to reload data from ViewModel
        viewModel.plan.value?.let { plan ->
            updateUIWithPlanData(plan)
        }
    }

    private fun updateUIWithPlanData(plan: com.datn.apptravel.data.model.Plan) {
        // Update UI with plan data
        binding.tvPlanTitle.text = getPlanTypeDisplayName(plan.type)
        binding.tvPlanName.text = plan.title
        binding.ivPlanIcon.setImageResource(getPlanTypeIcon(plan.type))

        // Update time
        displayTime(plan.startTime, plan.endTime, plan.type)

        // Update expense
        if ((plan.expense ?: 0.0) > 0) {
            binding.tvExpense.text = formatExpense(plan.expense!!)
        } else {
            binding.tvExpense.text = "0đ"
        }
        binding.tvExpense.visibility = View.VISIBLE
    }

    private fun showPlanMenu() {
        val popupMenu = PopupMenu(this, binding.btnMenu)
        popupMenu.menuInflater.inflate(R.menu.plan_detail_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_plan -> {
                    // Navigate to edit plan based on plan type
                    // Use data from Intent extras (passed from schedule list) instead of viewModel
                    val plan = viewModel.plan.value
                    if (plan != null) {
                        val planType = try {
                            val planTypeStr = intent.getStringExtra(EXTRA_PLAN_TYPE) ?: "OTHER"
                            PlanType.valueOf(planTypeStr)
                        } catch (e: Exception) {
                            PlanType.NONE
                        }
                        
                        val intent = when (planType) {
                            PlanType.RESTAURANT -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity::class.java)
                            PlanType.LODGING -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.LodgingDetailActivity::class.java)
                            PlanType.FLIGHT -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.FlightDetailActivity::class.java)
                            PlanType.BOAT -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.BoatDetailActivity::class.java)
                            PlanType.CAR_RENTAL -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.CarRentalDetailActivity::class.java)
                            PlanType.TRAIN -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.CarRentalDetailActivity::class.java)
                            PlanType.ACTIVITY, PlanType.TOUR, PlanType.THEATER, PlanType.SHOPPING,
                            PlanType.CAMPING, PlanType.RELIGION -> Intent(this, com.datn.apptravel.ui.trip.detail.plandetail.ActivityDetailActivity::class.java)
                            PlanType.NONE, PlanType.OTHER -> {
                                Toast.makeText(this, "Cannot edit this plan type", Toast.LENGTH_SHORT).show()
                                return@setOnMenuItemClickListener false
                            }
                        }
                        
                        // Pass plan data to edit activity
                        intent.putExtra("tripId", tripId)
                        intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_PLAN_ID, planId)
                        intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_PLAN_TITLE, plan.title)
                        intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_PLACE_ADDRESS, plan.address)
                        intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_START_TIME, plan.startTime)

                        // Pass plan type-specific fields
                        when (planType) {
                            PlanType.ACTIVITY, PlanType.TOUR, PlanType.THEATER, PlanType.SHOPPING,
                            PlanType.CAMPING, PlanType.RELIGION -> {
                                // ActivityPlan: has endTime
                                // Try to get from viewModel.plan first, fallback to Intent extra
                                val endTime = plan.endTime ?: this@PlanDetailActivity.intent.getStringExtra(EXTRA_END_TIME)
                                Log.d("PlanDetailActivity", "Edit Activity - plan.endTime: ${plan.endTime}, intent endTime: ${this@PlanDetailActivity.intent.getStringExtra(EXTRA_END_TIME)}, final: $endTime")
                                endTime?.let { intent.putExtra("end_time", it) }
                            }
                            PlanType.LODGING -> {
                                // LodgingPlan: has checkInDate, checkOutDate
                                val checkInDate = plan.checkInDate ?: this@PlanDetailActivity.intent.getStringExtra("checkInDate")
                                val checkOutDate = plan.checkOutDate ?: this@PlanDetailActivity.intent.getStringExtra("checkOutDate")
                                checkInDate?.let { intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_START_TIME, it) }
                                checkOutDate?.let { intent.putExtra("end_time", it) }
                                plan.phone?.let { intent.putExtra("phone", it) }
                            }
                            PlanType.RESTAURANT -> {
                                // RestaurantPlan: has reservationDate, reservationTime
                                val reservationDate = plan.reservationDate ?: this@PlanDetailActivity.intent.getStringExtra("reservationDate")
                                val reservationTime = plan.reservationTime ?: this@PlanDetailActivity.intent.getStringExtra("reservationTime")
                                reservationDate?.let { intent.putExtra("reservationDate", it) }
                                reservationTime?.let { intent.putExtra("reservationTime", it) }
                            }
                            PlanType.FLIGHT -> {
                                // FlightPlan: has arrivalLocation, arrivalAddress, arrivalDate
                                // arrivalDate is the second time (arrival time)
                                val arrivalDate = plan.arrivalDate ?: this@PlanDetailActivity.intent.getStringExtra("arrivalDate")
                                arrivalDate?.let {
                                    intent.putExtra("arrivalDate", it)
                                    intent.putExtra("end_time", it) // Also set as end_time for general use
                                }
                                plan.arrivalLocation?.let { intent.putExtra("arrivalLocation", it) }
                                plan.arrivalAddress?.let { intent.putExtra("arrivalAddress", it) }
                            }
                            PlanType.BOAT -> {
                                // BoatPlan: has arrivalTime, arrivalLocation, arrivalAddress
                                // arrivalTime is the second time
                                val arrivalTime = plan.arrivalTime ?: this@PlanDetailActivity.intent.getStringExtra("arrivalTime")
                                arrivalTime?.let {
                                    intent.putExtra("arrivalTime", it)
                                    intent.putExtra("end_time", it) // Also set as end_time for general use
                                }
                                plan.arrivalLocation?.let { intent.putExtra("arrivalLocation", it) }
                                plan.arrivalAddress?.let { intent.putExtra("arrivalAddress", it) }
                            }
                            PlanType.CAR_RENTAL, PlanType.TRAIN -> {
                                // CarRentalPlan: has pickupDate, pickupTime
                                plan.pickupDate?.let { intent.putExtra("pickupDate", it) }
                                plan.pickupTime?.let { intent.putExtra("pickupTime", it) }
                                plan.phone?.let { intent.putExtra("phone", it) }
                            }
                            else -> {
                                // Default: just pass endTime if available
                                plan.endTime?.let { intent.putExtra("end_time", it) }
                            }
                        }

                        intent.putExtra(com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity.EXTRA_EXPENSE, plan.expense ?: 0.0)
                        intent.putExtra("placeLatitude", 0.0) // Will be parsed from plan.location if needed
                        intent.putExtra("placeLongitude", 0.0)
                        intent.putExtra("planType", planType.name) // Pass plan type for editing

                        editPlanLauncher.launch(intent)
                    } else {
                        Toast.makeText(this, "Plan data not available", Toast.LENGTH_SHORT).show()
                    }
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
                deletePlan()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePlan() {
        val currentTripId = tripId ?: return
        val currentPlanId = planId ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = tripRepository.deletePlan(currentTripId, currentPlanId)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(this@PlanDetailActivity, "Plan deleted successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Notify previous screen to refresh
                    finish()
                } else {
                    Toast.makeText(
                        this@PlanDetailActivity,
                        "Failed to delete plan: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showDeletePhotoConfirmation(photoFileName: String, photoIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo?")
            .setPositiveButton("Delete") { _, _ ->
                deletePhotoFromPlan(photoFileName, photoIndex)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePhotoFromPlan(photoFileName: String, photoIndex: Int) {
        val currentTripId = tripId ?: return
        val currentPlanId = planId ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = tripRepository.deletePhotoFromPlan(currentTripId, currentPlanId, photoFileName)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    // Remove photo from adapter
                    photoAdapter.removePhoto(photoIndex)
                    Toast.makeText(this@PlanDetailActivity, "Photo deleted successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@PlanDetailActivity,
                        "Failed to delete photo: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                // Post comment through ViewModel
                viewModel.postComment(comment)
                Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()

        // Auto focus on EditText and show keyboard
        etComment.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun uploadPhotos(uris: List<Uri>) {
        if (planId != null && tripId != null) {
            viewModel.uploadPhotos(this, uris, tripId!!, planId!!)
        } else {
            Toast.makeText(
                this,
                "Cannot upload photos. Missing trip or plan ID.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
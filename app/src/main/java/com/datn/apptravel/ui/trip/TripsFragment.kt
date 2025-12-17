package com.datn.apptravel.ui.trip

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.databinding.FragmentTripsBinding
import com.datn.apptravel.ui.base.BaseFragment
import com.datn.apptravel.ui.trip.adapter.TripAdapter
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.TripsViewModel
import com.datn.apptravel.utils.ApiConfig
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TripsFragment : BaseFragment<FragmentTripsBinding, TripsViewModel>() {

    override val viewModel: TripsViewModel by viewModel()
    private val authRepository: AuthRepository by inject()

    // Track currently selected tab
    private var currentTab = TAB_ONGOING

    companion object {
        private const val TAB_ONGOING = 0
        private const val TAB_PAST = 1
        private const val TAB_COMMUNITY = 2

        // Intent extra keys
        const val EXTRA_TRIP_ID = "extra_trip_id"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTripsBinding =
        FragmentTripsBinding.inflate(inflater, container, false)

    // Trip adapter
    private lateinit var tripAdapter: TripAdapter

    override fun setupUI() {
        // Setup UI components
        loadUserName()
        setupAddTripButton()
        setupRecyclerViews()
        observeTrips()
        loadTrips()
    }

    /**
     * Load and display user name
     */
    private fun loadUserName() {
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                user?.let {
                    val displayName = if (it.lastName.isNotEmpty()) {
                        it.lastName
                    } else if (it.firstName.isNotEmpty()) {
                        it.firstName
                    } else {
                        "User"
                    }
                    binding.tvUserName.text = displayName
                    
                    // Load profile picture
                    if (!it.profilePicture.isNullOrEmpty()) {
                        Glide.with(this@TripsFragment)
                            .load(it.profilePicture)
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .into(binding.ivProfile)
                    } else {
                        // Set default icon if no profile picture
                        binding.ivProfile.setImageResource(R.drawable.ic_user)
                    }
                }
            }
        }
    }

    private fun setupAddTripButton() {
        binding.btnAddTripNow.setOnClickListener {
            // Navigate to CreateTripActivity
            val intent = Intent(requireContext(), CreateTripActivity::class.java)
            startActivity(intent)
        }

        binding.tvViewAll?.setOnClickListener {
            // Show all past trips
        }
    }

    /**
     * Load trips data based on current tab
     */
    private fun loadTrips() {
        viewModel.getTrips()
    }

    /**
     * Refresh trips data
     */
    fun refreshTrips() {
        viewModel.getTrips()
    }

    /**
     * Setup RecyclerViews for Adventure and Past Trips
     */
    private fun setupRecyclerViews() {
        // Setup Adventure RecyclerView (horizontal) for upcoming trips
        val adventureAdapter = TripAdapter(
            emptyList()
        ) { trip ->
            navigateToTripDetail(trip)
        }
        
        binding.rvAdventure.apply {
            adapter = adventureAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Setup Past Trips RecyclerView (horizontal)
        tripAdapter = TripAdapter(
            emptyList()
        ) { trip ->
            navigateToTripDetail(trip)
        }

        binding.rvPastTrips.apply {
            adapter = tripAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun navigateToTripDetail(trip: Trip) {
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    // Kiểm tra xem userId hiện tại có trùng với userId của trip không
                    if (user.id == trip.userId) {
                        // Người dùng là chủ trip -> điều hướng đến TripDetailActivity
                        val intent = Intent(requireContext(), TripDetailActivity::class.java).apply {
                            putExtra(EXTRA_TRIP_ID, trip.id.toString())
                        }
                        startActivity(intent)
                    } else {
                        // Người dùng không phải chủ trip -> điều hướng đến TripMapActivity
                        val intent = Intent(requireContext(), com.datn.apptravel.ui.trip.map.TripMapActivity::class.java).apply {
                            putExtra("tripId", trip.id.toString())
                            putExtra("tripTitle", trip.title)
                            putExtra("tripUserId", trip.userId)
                        }
                        startActivity(intent)
                    }
                }
                // Chỉ cần collect một lần
                return@collect
            }
        }
    }

    private fun observeTrips() {
        // Observe ongoing trips to show current trip
        viewModel.ongoingTrips.observe(viewLifecycleOwner) { ongoingTrips ->
            val upcomingTrips = viewModel.upcomingTrips.value ?: emptyList()
            updateCurrentTripCard(ongoingTrips, upcomingTrips)
            updateUpcomingsSection(ongoingTrips, upcomingTrips)
        }
        
        // Observe upcoming trips (startDate > today) for adventure section
        viewModel.upcomingTrips.observe(viewLifecycleOwner) { upcomingTrips ->
            val ongoingTrips = viewModel.ongoingTrips.value ?: emptyList()
            updateCurrentTripCard(ongoingTrips, upcomingTrips)
            updateUpcomingsSection(ongoingTrips, upcomingTrips)
        }
        
        // Observe past trips (endDate < today)
        viewModel.pastTrips.observe(viewLifecycleOwner) { trips ->
            if (trips.isNotEmpty()) {
                tripAdapter.updateTrips(trips)
                binding.rvPastTrips.visibility = View.VISIBLE
            } else {
                binding.rvPastTrips.visibility = View.GONE
            }
        }
    }
    
    private fun updateCurrentTripCard(ongoingTrips: List<Trip>, upcomingTrips: List<Trip>) {
        // Priority: ongoing trip first, then nearest upcoming trip
        val currentTrip = ongoingTrips.firstOrNull() ?: upcomingTrips.firstOrNull()
        
        if (currentTrip != null) {
            // Set title based on trip status
            binding.tvCurrentTripTitle.visibility = View.VISIBLE
            binding.tvCurrentTripTitle.text = if (ongoingTrips.isNotEmpty()) "Live Trip" else "Next Trip"
            
            // Show current trip card, hide "Where to Next"
            binding.currentTripCard.root.visibility = View.VISIBLE
            binding.cardWhereToNext?.visibility = View.GONE
            
            displayCurrentTrip(currentTrip, ongoingTrips.isNotEmpty())
        } else {
            // Hide title and current trip card, show "Where to Next"
            binding.tvCurrentTripTitle.visibility = View.GONE
            binding.currentTripCard.root.visibility = View.GONE
            binding.cardWhereToNext?.visibility = View.VISIBLE
        }
    }
    
    private fun updateUpcomingsSection(ongoingTrips: List<Trip>, upcomingTrips: List<Trip>) {
        // Exclude the trip shown in current trip card from Upcomings list
        val tripsForUpcomingsSection = if (ongoingTrips.isEmpty() && upcomingTrips.isNotEmpty()) {
            // If no ongoing trips, exclude the first upcoming (shown in current trip card)
            upcomingTrips.drop(1)
        } else {
            // If there's an ongoing trip, show all upcoming trips
            upcomingTrips
        }
        
        if (tripsForUpcomingsSection.isNotEmpty()) {
            binding.tvUpcomingsTitle.text = "Upcomings"
            val adventureAdapter = binding.rvAdventure.adapter as? TripAdapter
            adventureAdapter?.updateTrips(tripsForUpcomingsSection)
            binding.rvAdventure.visibility = View.VISIBLE
        } else {
            binding.tvUpcomingsTitle.text = "Adventure"
            binding.rvAdventure.visibility = View.GONE
        }
    }
    
    private fun displayCurrentTrip(trip: Trip, isOngoing: Boolean) {
        val root = binding.currentTripCard.root
        val liveBadge = root.findViewById<LinearLayout>(R.id.liveBadge)
        val badgeText = liveBadge.findViewById<TextView>(R.id.tvBadgeText)
        val badgeDot = liveBadge.findViewById<View>(R.id.badgeDot)
        val tripName = root.findViewById<TextView>(R.id.tvCurrentTripName)
        val dayProgress = root.findViewById<TextView>(R.id.tvDayProgress)
        val plansCount = root.findViewById<TextView>(R.id.tvPlansCount)
        val tripImage = root.findViewById<ImageView>(R.id.ivCurrentTripImage)
        
        // Show badge and set text/color based on trip status
        liveBadge.visibility = View.VISIBLE
        if (isOngoing) {
            badgeText.text = "LIVE"
            badgeText.setTextColor(android.graphics.Color.parseColor("#EF4444"))
            badgeDot.setBackgroundResource(R.drawable.bg_live_dot)
            liveBadge.setBackgroundResource(R.drawable.bg_live_badge)
        } else {
            badgeText.text = "READY"
            badgeText.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
            badgeDot.setBackgroundResource(R.drawable.bg_ready_dot)
            liveBadge.setBackgroundResource(R.drawable.bg_ready_badge)
        }

        
        // Set trip name
        tripName.text = trip.title
        
        // Calculate day progress or days until start
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = LocalDate.now()
            val startDate = LocalDate.parse(trip.startDate, dateFormatter)
            val endDate = LocalDate.parse(trip.endDate, dateFormatter)
            val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            
            if (isOngoing) {
                // Calculate current day
                val currentDay = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
                dayProgress.text = "Ngày $currentDay"
            } else {
                // Calculate days until start
                val daysUntil = ChronoUnit.DAYS.between(today, startDate).toInt()
                dayProgress.text = if (daysUntil == 0) {
                    "Bắt đầu hôm nay"
                } else if (daysUntil == 1) {
                    "Bắt đầu sau 1 ngày nữa"
                } else {
                    "Bắt đầu sau $daysUntil ngày nữa"
                }
            }
        } catch (e: Exception) {
            dayProgress.text = ""
        }
        
        // Set plans count based on trip status
        val plansNumber = if (isOngoing) {
            // Trip is ongoing - show today's plans only
            try {
                val today = LocalDate.now()
                trip.plans?.count { plan ->
                    try {
                        // Parse plan startTime to get the date
                        val planDateTime = LocalDateTime.parse(
                            plan.startTime,
                            DateTimeFormatter.ISO_DATE_TIME
                        )
                        val planDate = planDateTime.toLocalDate()
                        // Check if plan is today
                        planDate.isEqual(today)
                    } catch (e: Exception) {
                        android.util.Log.e("TripsFragment", "Error parsing plan date: ${plan.title}", e)
                        false
                    }
                } ?: 0
            } catch (e: Exception) {
                android.util.Log.e("TripsFragment", "Error counting today's plans", e)
                trip.plans?.size ?: 0
            }
        } else {
            // Trip hasn't started - show total plans
            trip.plans?.size ?: 0
        }
        plansCount.text = "$plansNumber plans"
        
        // Debug log
        android.util.Log.d("TripsFragment", "Trip: ${trip.title}, Plans: $plansNumber, Plans data: ${trip.plans}")
        
        // Load trip image
        val imageUrl = ApiConfig.getImageUrl(trip.coverPhoto)
        if (imageUrl != null) {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.bg_a)
                .error(R.drawable.bg_a)
                .centerCrop()
                .into(tripImage)
        } else {
            tripImage.setImageResource(R.drawable.bg_a)
        }
        
        // Set click listener
        root.setOnClickListener {
            navigateToTripDetail(trip)
        }
    }

    override fun handleLoading(isLoading: Boolean) {
        // Show/hide loading indicator
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
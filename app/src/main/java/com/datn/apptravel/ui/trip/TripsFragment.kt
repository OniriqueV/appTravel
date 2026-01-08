package com.datn.apptravels.ui.trip

import android.app.Dialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.data.model.AdventureItem
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.repository.AuthRepository
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.databinding.FragmentTripsBinding
import com.datn.apptravels.ui.base.BaseFragment
import com.datn.apptravels.ui.discover.model.DiscoverItem
import com.datn.apptravels.ui.trip.adapter.TripAdapter
import com.datn.apptravels.ui.trip.adapter.DiscoverTripAdapter
import com.datn.apptravels.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravels.ui.trip.viewmodel.TripsViewModel
import com.datn.apptravels.utils.ApiConfig
import com.datn.apptravels.ui.trip.map.TripMapActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TripsFragment : BaseFragment<FragmentTripsBinding, TripsViewModel>() {

    override val viewModel: TripsViewModel by activityViewModel()
    private val authRepository: AuthRepository by inject()
    private val tripRepository: TripRepository by inject()
    private val sessionManager: SessionManager by inject()
    
    // Track adventure section state
    private enum class AdventureSectionState {
        UPCOMING_TRIPS,  // Đang hiển thị upcoming trips của user
        DISCOVER_TRIPS   // Đang hiển thị discover trips từ community
    }
    private var currentAdventureState: AdventureSectionState? = AdventureSectionState.DISCOVER_TRIPS
    
    // Activity result launcher for CreateTripActivity
    private val createTripLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Trip created successfully, refresh data
            android.util.Log.d("TripsFragment", "Trip created, refreshing data")
            viewModel.refreshTrips()
        }
    }

    companion object {
        // Intent extra keys (not used but kept for compatibility)
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
        setupSwipeRefresh()
        setupAddTripButton()
        setupRecyclerViews()
        observeTrips()
        
        // No need to call getTrips() here - MainActivity already loaded data
        // LiveData will automatically emit cached data to new observers
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            android.util.Log.d("TripsFragment", "Swipe to refresh triggered")
            viewModel.refreshTrips()
        }
        
        // Customize refresh colors
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#C9A877")
        )
    }


    private fun loadUserName() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TripsFragment", "loadUserName: Starting to collect currentUser")
                
                // Observe currentUser Flow để luôn cập nhật khi user data thay đổi
                authRepository.currentUser.collect { user ->
                    android.util.Log.d("TripsFragment", "loadUserName: Received user data - id: ${user?.id}, firstName: ${user?.firstName}, lastName: ${user?.lastName}")
                    
                    // Check if fragment is still attached
                    if (!isAdded || context == null) {
                        android.util.Log.w("TripsFragment", "loadUserName: Fragment detached, stopping")
                        return@collect
                    }
                    
                    if (user != null) {
                        val displayName = when {
                            user.lastName.isNotEmpty() -> {
                                android.util.Log.d("TripsFragment", "loadUserName: Using lastName: ${user.lastName}")
                                user.lastName
                            }
                            user.firstName.isNotEmpty() -> {
                                android.util.Log.d("TripsFragment", "loadUserName: Using firstName: ${user.firstName}")
                                user.firstName
                            }
                            else -> {
                                android.util.Log.w("TripsFragment", "loadUserName: No name available, using default 'User'")
                                "User"
                            }
                        }
                        binding.tvUserName.text = displayName
                        
                        // Load profile picture
                        if (!user.profilePicture.isNullOrEmpty()) {
                            android.util.Log.d("TripsFragment", "loadUserName: Loading profile picture: ${user.profilePicture}")
                            Glide.with(this@TripsFragment)
                                .load(user.profilePicture)
                                .placeholder(R.drawable.ic_user)
                                .error(R.drawable.ic_user)
                                .into(binding.ivProfile)
                        } else {
                            android.util.Log.d("TripsFragment", "loadUserName: No profile picture, using default icon")
                            binding.ivProfile.setImageResource(R.drawable.ic_user)
                        }
                    } else {
                        android.util.Log.w("TripsFragment", "loadUserName: User is null")
                        binding.tvUserName.text = "User"
                        binding.ivProfile.setImageResource(R.drawable.ic_user)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TripsFragment", "Error loading user name", e)
                // Set default values on error
                if (isAdded && context != null) {
                    binding.tvUserName.text = "User"
                    binding.ivProfile.setImageResource(R.drawable.ic_user)
                }
            }
        }
    }

    private fun setupAddTripButton() {
        binding.btnAddTripNow.setOnClickListener {
            // Navigate to CreateTripActivity using launcher
            val intent = Intent(requireContext(), CreateTripActivity::class.java)
            createTripLauncher.launch(intent)
        }

//        binding.tvViewAll?.setOnClickListener {
//            // Show all past trips
//        }
    }

    private fun loadTrips() {

            viewModel.getTrips()

    }

    fun refreshTrips() {
        android.util.Log.d("TripsFragment", "refreshTrips() called - forcing refresh")
        viewModel.refreshTrips()
    }

    private fun setupRecyclerViews() {
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
            try {
                val user = authRepository.currentUser.firstOrNull()
                
                // Check if fragment is still attached
                if (!isAdded || context == null) {
                    return@launch
                }
                
                if (user != null) {
                    val isOwner = user.id == trip.userId
                    val isMember = trip.members?.any { it.id == user.id } == true
                    
                    // DEBUG LOG
                    android.util.Log.d("TripsFragment", "=== Navigation Debug ===")
                    android.util.Log.d("TripsFragment", "Trip: ${trip.title} (id: ${trip.id})")
                    android.util.Log.d("TripsFragment", "Trip userId: ${trip.userId} (type: ${trip.userId::class.simpleName})")
                    android.util.Log.d("TripsFragment", "Current user id: ${user.id} (type: ${user.id::class.simpleName})")
                    android.util.Log.d("TripsFragment", "Members: ${trip.members?.map { "${it.firstName} (${it.id})" }}")
                    android.util.Log.d("TripsFragment", "isOwner: $isOwner, isMember: $isMember")
                    android.util.Log.d("TripsFragment", "======================")
                    
                    // Owner hoặc member đều vào TripDetailActivity
                    // TripDetailActivity sẽ tự xử lý permissions
                    if (isOwner || isMember) {
                        android.util.Log.d("TripsFragment", "✅ Navigating to TripDetailActivity (owner or member)")
                        val intent = Intent(requireContext(), TripDetailActivity::class.java).apply {
                            putExtra(EXTRA_TRIP_ID, trip.id.toString())
                        }
                        startActivity(intent)
                    } else {
                        android.util.Log.d("TripsFragment", "➡️ Navigating to TripMapActivity (not owner/member)")
                        // Người ngoài (không phải owner/member) -> redirect sang TripMapActivity
                        val intent = Intent(requireContext(), com.datn.apptravels.ui.trip.map.TripMapActivity::class.java).apply {
                            putExtra("tripId", trip.id.toString())
                            putExtra("tripTitle", trip.title)
                            putExtra("tripUserId", trip.userId)
                        }
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TripsFragment", "Error navigating to trip detail", e)
            }
        }
    }

    private fun observeTrips() {
        val combinedTripsObserver =
            androidx.lifecycle.MediatorLiveData<Pair<List<Trip>, List<Trip>>>()

        combinedTripsObserver.addSource(viewModel.ongoingTrips) { ongoing ->
            val upcoming = viewModel.upcomingTrips.value ?: emptyList()
            combinedTripsObserver.value = Pair(ongoing, upcoming)
        }

        combinedTripsObserver.addSource(viewModel.upcomingTrips) { upcoming ->
            val ongoing = viewModel.ongoingTrips.value ?: emptyList()
            combinedTripsObserver.value = Pair(ongoing, upcoming)
        }

        // Observe combined data to ensure consistency
        combinedTripsObserver.observe(viewLifecycleOwner) { (ongoingTrips, upcomingTrips) ->
            Log.d("TripsFragment", "Observer triggered - Ongoing: ${ongoingTrips.size}, Upcoming: ${upcomingTrips.size}")
            updateCurrentTripCard(ongoingTrips, upcomingTrips)
            updateUpcomingsSection(ongoingTrips, upcomingTrips)
        }

        // Observe past trips (endDate < today)
        viewModel.pastTrips.observe(viewLifecycleOwner) { trips ->
            Log.d("TripsFragment", "Past trips observer - Count: ${trips.size}")
            if (trips.isNotEmpty()) {
                tripAdapter.updateTrips(trips)
                binding.rvPastTrips.visibility = View.VISIBLE
                Log.d("TripsFragment", "Past trips section: VISIBLE")
            } else {
                binding.rvPastTrips.visibility = View.GONE
                Log.d("TripsFragment", "Past trips section: GONE")
            }
        }

        // Observe adventure items (optimized API - replaces filteredDiscoverTrips)
        viewModel.adventureItems.observe(viewLifecycleOwner) { adventureItems ->
            Log.d("TripsFragment", "Adventure items observer - Count: ${adventureItems.size}, State: $currentAdventureState")
            // Only render if we're in DISCOVER_TRIPS state
            if (currentAdventureState == AdventureSectionState.DISCOVER_TRIPS) {
                renderAdventureTrips(adventureItems)
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
        android.util.Log.d("TripsFragment", "updateUpcomingsSection - ongoing: ${ongoingTrips.size}, upcoming: ${upcomingTrips.size}")

        val userUpcomingTrips = when {
            ongoingTrips.isNotEmpty() -> upcomingTrips // Có ongoing -> show all upcoming
            upcomingTrips.isNotEmpty() -> upcomingTrips.drop(1) // Không có ongoing -> skip first upcoming
            else -> emptyList()
        }
        
        android.util.Log.d("TripsFragment", "User upcoming trips to show: ${userUpcomingTrips.size}")

        // Quyết định hiển thị gì
        if (userUpcomingTrips.isNotEmpty()) {
            renderUpcomingTripsSection(userUpcomingTrips)
        } else {
            renderDiscoverTripsSection()
        }
    }

    private fun renderUpcomingTripsSection(trips: List<Trip>) {
        android.util.Log.d("TripsFragment", "renderUpcomingTripsSection with ${trips.size} trips")
        
        // Set state
        currentAdventureState = AdventureSectionState.UPCOMING_TRIPS
        
        // Set title
        binding.tvUpcomingsTitle.text = "Upcomings"
        
        // Remove padding for Upcomings
        binding.rvAdventure.setPadding(0, 0, 0, 0)
        
        // Create and set adapter
        val adapter = TripAdapter(trips) { trip ->
            navigateToTripDetail(trip)
        }
        binding.rvAdventure.adapter = adapter
        binding.rvAdventure.visibility = View.VISIBLE
    }

    private fun renderDiscoverTripsSection() {
        android.util.Log.d("TripsFragment", "renderDiscoverTripsSection")
        
        // Set state FIRST so observer can trigger
        currentAdventureState = AdventureSectionState.DISCOVER_TRIPS

        binding.tvUpcomingsTitle.text = "Adventure"

        // Check if adventure data is already available
        val cachedAdventureItems = viewModel.adventureItems.value
        if (cachedAdventureItems != null && cachedAdventureItems.isNotEmpty()) {
            // Data available immediately - render it
            android.util.Log.d("TripsFragment", "Adventure data available, rendering immediately")
            renderAdventureTrips(cachedAdventureItems)
        } else {
            // No data yet - hide view and trigger load
            android.util.Log.d("TripsFragment", "No adventure data, triggering load")
            binding.rvAdventure.visibility = View.GONE
            // Observer will render when data arrives
        }
    }

    private fun renderAdventureTrips(adventureItems: List<AdventureItem>) {
        // Check fragment still attached
        if (!isAdded || context == null) {
            android.util.Log.w("TripsFragment", "Fragment detached, skip rendering")
            return
        }

        if (currentAdventureState != AdventureSectionState.DISCOVER_TRIPS) {
            android.util.Log.w("TripsFragment", "State changed, skip rendering adventure trips")
            return
        }

        android.util.Log.d("TripsFragment", "Rendering ${adventureItems.size} adventure items")

        if (adventureItems.isNotEmpty()) {
            // Add padding for Adventure section (peek effect)
            val paddingInDp = 40
            val paddingInPx = (paddingInDp * resources.displayMetrics.density).toInt()
            binding.rvAdventure.setPadding(paddingInPx, 0, paddingInPx, 0)
            
            // Convert AdventureItems to DiscoverItems for adapter compatibility
            val discoverItems = adventureItems.map { adventureItem ->
                DiscoverItem(
                    tripId = adventureItem.tripId,
                    userId = adventureItem.trip.userId,
                    userName = "${adventureItem.user.firstName} ${adventureItem.user.lastName}",
                    userAvatar = adventureItem.user.profilePicture,
                    tripImage = adventureItem.trip.coverPhoto,
                    caption = adventureItem.trip.content,
                    tags = adventureItem.trip.tags,
                    isFollowing = false, // This info is not in adventure response yet
                    isPublic = adventureItem.trip.isPublic ?: "none",
                    sharedAt = adventureItem.trip.sharedAt ?: ""
                )
            }
            
            val adapter = DiscoverTripAdapter(discoverItems, tripRepository, sessionManager) { item ->
                handleDiscoverTripClick(item)
            }
            binding.rvAdventure.adapter = adapter
            binding.rvAdventure.visibility = View.VISIBLE
            android.util.Log.d("TripsFragment", "Rendered ${adventureItems.size} adventure trips")
        } else {
            binding.rvAdventure.visibility = View.GONE
            android.util.Log.d("TripsFragment", "No adventure trips, hiding recycler view")
        }
    }
    
    private fun handleDiscoverTripClick(item: DiscoverItem) {
        if (!isAdded || context == null) return
        
        // Get trip details from cache for map navigation
        val cachedDetail = sessionManager.getCachedDiscoverTripDetail(item.tripId)
        val intent = Intent(requireContext(), TripMapActivity::class.java).apply {
            putExtra("tripId", item.tripId)
            putExtra("tripTitle", cachedDetail?.trip?.content ?: "Adventure Trip")
            putExtra("tripUserId", cachedDetail?.trip?.userId)
        }
        startActivity(intent)
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
        val warningBadge = root.findViewById<ImageView>(R.id.ivCurrentTripWarningBadge)
        
        // Show/hide warning badge based on conflict status
        warningBadge?.visibility = if (trip.hasConflict) View.VISIBLE else View.GONE
        
        // Set click listener for warning badge
        warningBadge?.setOnClickListener {
            showConflictDialog()
        }
        
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
    
    private fun showConflictDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_trip_conflict)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val ivClose = dialog.findViewById<ImageView>(R.id.ivClose)
        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun handleLoading(isLoading: Boolean) {
        // Show/hide loading indicator
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        // Stop swipe refresh animation if it's running
        if (!isLoading && binding.swipeRefresh.isRefreshing) {
            binding.swipeRefresh.isRefreshing = false
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Don't reset isInitialLoadComplete here - we want to keep the flag
        // It will be reset when the app is truly closed/recreated
    }
}
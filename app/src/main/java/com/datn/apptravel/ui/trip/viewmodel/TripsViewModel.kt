package com.datn.apptravels.ui.trip.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.data.local.CachedDiscoverTripDetail
import com.datn.apptravels.data.model.AdventureItem
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class TripsViewModel(
    private val tripRepository: TripRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _trips = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<Trip>> = _trips
    
    private val _ongoingTrips = MutableLiveData<List<Trip>>()
    val ongoingTrips: LiveData<List<Trip>> = _ongoingTrips
    
    private val _upcomingTrips = MutableLiveData<List<Trip>>()
    val upcomingTrips: LiveData<List<Trip>> = _upcomingTrips
    
    private val _pastTrips = MutableLiveData<List<Trip>>()
    val pastTrips: LiveData<List<Trip>> = _pastTrips
    
    // Adventure items (optimized - all data in 1 API call)
    private val _adventureItems = MutableLiveData<List<AdventureItem>>()
    val adventureItems: LiveData<List<AdventureItem>> = _adventureItems
    
    // Cache flags
    var isDataLoaded = false
        private set
    private var lastLoadTime = 0L
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes cache

    fun getTrips(forceRefresh: Boolean = false) {
//        setLoading(true)

        // Check if data is already loaded and not expired (unless force refresh)
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && isDataLoaded && (currentTime - lastLoadTime) < CACHE_DURATION) {
            android.util.Log.d("TripsViewModel", "Using cached data, age: ${(currentTime - lastLoadTime) / 1000}s")
            // Re-post current values to trigger observers for UI updates
            // Use toList() to create new list instances to ensure observers trigger
            _trips.value = _trips.value?.toList()
            _ongoingTrips.value = _ongoingTrips.value?.toList()
            _upcomingTrips.value = _upcomingTrips.value?.toList()
            _pastTrips.value = _pastTrips.value?.toList()
            _adventureItems.value = _adventureItems.value?.toList()
            setLoading(false)
            return
        }

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    android.util.Log.d("TripsViewModel", "Fetching trips for userId: $userId")
                    
                    // Fetch user trips IN PARALLEL for faster loading
                    val ownTripsDeferred = async(Dispatchers.IO) { tripRepository.getTripsByUserId(userId) }
                    val memberTripsDeferred = async(Dispatchers.IO) { tripRepository.getTripsByMemberId(userId) }

                    val ownTripsResult = ownTripsDeferred.await()
                    val memberTripsResult = memberTripsDeferred.await()
                    
                    val ownTrips = ownTripsResult.getOrNull() ?: emptyList()
                    val memberTrips = memberTripsResult.getOrNull() ?: emptyList()
                    
                    android.util.Log.d("TripsViewModel", "Own trips count: ${ownTrips.size}")
                    android.util.Log.d("TripsViewModel", "Member trips count: ${memberTrips.size}")
                    
                    // Log any errors from fetching member trips
                    memberTripsResult.onFailure { exception ->
                        android.util.Log.e("TripsViewModel", "Error fetching member trips: ${exception.message}", exception)
                    }
                    
                    // Merge both lists
                    val allTrips = (ownTrips + memberTrips).toMutableList()
                    android.util.Log.d("TripsViewModel", "Total trips after merge: ${allTrips.size}")
                    
                    // Detect time conflicts
                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    for (i in allTrips.indices) {
                        for (j in i + 1 until allTrips.size) {
                            try {
                                val trip1StartDate = LocalDate.parse(allTrips[i].startDate, dateFormatter)
                                val trip1EndDate = LocalDate.parse(allTrips[i].endDate, dateFormatter)
                                val trip2StartDate = LocalDate.parse(allTrips[j].startDate, dateFormatter)
                                val trip2EndDate = LocalDate.parse(allTrips[j].endDate, dateFormatter)
                                
                                // Check if trips overlap
                                val overlaps = !(trip1EndDate.isBefore(trip2StartDate) || trip2EndDate.isBefore(trip1StartDate))
                                if (overlaps) {
                                    allTrips[i].hasConflict = true
                                    allTrips[j].hasConflict = true
                                }
                            } catch (e: Exception) {
                                // Ignore parsing errors
                            }
                        }
                    }
                    
                    _trips.value = allTrips
                    
                    // Filter trips by date
                    val today = LocalDate.now()
                    
                    val ongoing = mutableListOf<Trip>()
                    val upcoming = mutableListOf<Trip>()
                    val past = mutableListOf<Trip>()
                    
                    allTrips.forEach { trip ->
                        try {
                            val startDate = LocalDate.parse(trip.startDate, dateFormatter)
                            val endDate = LocalDate.parse(trip.endDate, dateFormatter)
                            
                            when {
                                endDate.isBefore(today) -> {
                                    // Trip ended before today -> past trip
                                    past.add(trip)
                                }
                                startDate.isAfter(today) -> {
                                    // Trip starts in the future -> upcoming trip
                                    upcoming.add(trip)
                                }
                                else -> {
                                    // Trip is happening now (start <= today <= end) -> ongoing trip
                                    ongoing.add(trip)
                                }
                            }
                        } catch (e: Exception) {
                            // If date parsing fails, add to ongoing by default
                            ongoing.add(trip)
                        }
                    }
                    
                    // Sort past trips (travel memory) by endDate descending (most recent end date first)
                    val sortedPast = past.sortedByDescending { trip ->
                        try {
                            LocalDate.parse(trip.endDate, dateFormatter)
                        } catch (e: Exception) {
                            LocalDate.MIN // Put invalid dates at the end
                        }
                    }
                    
                    // Sort upcoming trips by startDate ascending (nearest start date first)
                    val sortedUpcoming = upcoming.sortedBy { trip ->
                        try {
                            LocalDate.parse(trip.startDate, dateFormatter)
                        } catch (e: Exception) {
                            LocalDate.MAX // Put invalid dates at the end
                        }
                    }
                    
                    // Sort ongoing trips by startDate ascending (started earliest first)
                    val sortedOngoing = ongoing.sortedBy { trip ->
                        try {
                            LocalDate.parse(trip.startDate, dateFormatter)
                        } catch (e: Exception) {
                            LocalDate.MAX // Put invalid dates at the end
                        }
                    }
                    
                    _ongoingTrips.value = sortedOngoing
                    _upcomingTrips.value = sortedUpcoming
                    _pastTrips.value = sortedPast
                    
                    // Debug log to verify trip categorization
                    android.util.Log.d("TripsViewModel", "Ongoing trips: ${sortedOngoing.size}")
                    android.util.Log.d("TripsViewModel", "Upcoming trips: ${sortedUpcoming.size}")
                    android.util.Log.d("TripsViewModel", "Past trips: ${sortedPast.size}")
                    sortedOngoing.forEach { android.util.Log.d("TripsViewModel", "  Ongoing: ${it.title} (${it.startDate} to ${it.endDate})") }
                    sortedUpcoming.forEach { android.util.Log.d("TripsViewModel", "  Upcoming: ${it.title} (${it.startDate} to ${it.endDate})") }
                    sortedPast.forEach { android.util.Log.d("TripsViewModel", "  Past: ${it.title} (${it.startDate} to ${it.endDate})") }

                    // Fetch adventure trips using optimized API (1 call instead of 11)
                    android.util.Log.d("TripsViewModel", "Fetching adventure trips with optimized API")
                    val adventureResult = tripRepository.getAdventureTrips(userId, 10)
                    
                    if (adventureResult.isSuccess) {
                        val adventureResponse = adventureResult.getOrNull()
                        if (adventureResponse != null) {
                            // Cache trip details for later use
                            adventureResponse.items.forEach { item ->
                                try {
                                    // Convert TripDetail to Trip model
                                    val trip = Trip(
                                        id = item.trip.id,
                                        userId = item.trip.userId,
                                        title = item.trip.title,
                                        startDate = item.trip.startDate,
                                        endDate = item.trip.endDate,
                                        isPublic = item.trip.isPublic ?: "none",
                                        coverPhoto = item.trip.coverPhoto,
                                        content = item.trip.content,
                                        tags = item.trip.tags,
                                        plans = item.trip.plans,
                                        members = item.trip.members,
                                        sharedWithUsers = item.trip.sharedWithUsers,
                                        createdAt = null,
                                        sharedAt = null
                                    )
                                    
                                    // Convert UserDetail to User model
                                    val user = com.datn.apptravels.data.model.User(
                                        id = item.user.id,
                                        firstName = item.user.firstName,
                                        lastName = item.user.lastName,
                                        email = item.user.email ?: "",
                                        role = item.user.role ?: "user",
                                        profilePicture = item.user.profilePicture,
                                        provider = null,
                                        providerId = null,
                                        enabled = true,
                                        createdAt = null,
                                        updatedAt = null
                                    )
                                    
                                    val cachedDetail = CachedDiscoverTripDetail(
                                        trip = trip,
                                        user = user,
                                        duration = item.duration,
                                        startDateText = item.startDateText,
                                        durationText = item.durationText
                                    )
                                    sessionManager.cacheDiscoverTripDetail(item.tripId, cachedDetail)
                                } catch (e: Exception) {
                                    android.util.Log.e("TripsViewModel", "Error caching adventure item ${item.tripId}", e)
                                }
                            }
                            
                            _adventureItems.value = adventureResponse.items
                            android.util.Log.d("TripsViewModel", "Loaded ${adventureResponse.items.size} adventure items")
                        }
                    } else {
                        android.util.Log.e("TripsViewModel", "Failed to fetch adventure trips", adventureResult.exceptionOrNull())
                        _adventureItems.value = emptyList()
                    }
                    
                    // Mark data as loaded with timestamp
                    isDataLoaded = true
                    lastLoadTime = System.currentTimeMillis()
                    android.util.Log.d("TripsViewModel", "Data loaded and cached at ${lastLoadTime}")
                    
                } else {
                    _trips.value = emptyList()
                    _ongoingTrips.value = emptyList()
                    _upcomingTrips.value = emptyList()
                    _pastTrips.value = emptyList()
                    _adventureItems.value = emptyList()
                }
            } catch (e: Exception) {
                setError(e.message ?: "An error occurred")
                _trips.value = emptyList()
                _ongoingTrips.value = emptyList()
                _upcomingTrips.value = emptyList()
                _pastTrips.value = emptyList()
                _adventureItems.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }

    fun refreshTrips() {
        android.util.Log.d("TripsViewModel", "Force refreshing trips data")
        getTrips(forceRefresh = true)
    }
    

    fun invalidateCache() {
        android.util.Log.d("TripsViewModel", "Cache invalidated")
        isDataLoaded = false
        lastLoadTime = 0L
    }
}
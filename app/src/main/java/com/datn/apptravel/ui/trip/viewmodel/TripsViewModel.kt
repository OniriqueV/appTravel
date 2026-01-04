package com.datn.apptravel.ui.trip.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    fun getTrips() {
        setLoading(true)

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    android.util.Log.d("TripsViewModel", "Fetching trips for userId: $userId")
                    
                    // Fetch both trips created by user and trips where user is a member
                    val ownTripsResult = tripRepository.getTripsByUserId(userId)
                    val memberTripsResult = tripRepository.getTripsByMemberId(userId)
                    
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
                } else {
                    _trips.value = emptyList()
                    _ongoingTrips.value = emptyList()
                    _upcomingTrips.value = emptyList()
                    _pastTrips.value = emptyList()
                }
            } catch (e: Exception) {
                setError(e.message ?: "An error occurred")
                _trips.value = emptyList()
                _ongoingTrips.value = emptyList()
                _upcomingTrips.value = emptyList()
                _pastTrips.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }
}
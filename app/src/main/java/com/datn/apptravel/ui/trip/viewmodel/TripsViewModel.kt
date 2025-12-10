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
                    val result = tripRepository.getTripsByUserId(userId)
                    result.onSuccess { tripList ->
                        _trips.value = tripList
                        
                        // Filter trips by end date
                        val today = LocalDate.now()
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        
                        val ongoing = mutableListOf<Trip>()
                        val upcoming = mutableListOf<Trip>()
                        val past = mutableListOf<Trip>()
                        
                        tripList.forEach { trip ->
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
                        
                        // Sort past trips by endDate descending (most recent first)
                        val sortedPast = past.sortedByDescending { trip ->
                            try {
                                LocalDate.parse(trip.endDate, dateFormatter)
                            } catch (e: Exception) {
                                LocalDate.MIN // Put invalid dates at the end
                            }
                        }
                        
                        // Sort upcoming trips by startDate ascending (nearest first)
                        val sortedUpcoming = upcoming.sortedBy { trip ->
                            try {
                                LocalDate.parse(trip.startDate, dateFormatter)
                            } catch (e: Exception) {
                                LocalDate.MAX // Put invalid dates at the end
                            }
                        }
                        
                        // Sort ongoing trips by startDate ascending (nearest first)
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
                    }.onFailure { exception ->
                        setError(exception.message ?: "Failed to load trips")
                        _trips.value = emptyList()
                        _ongoingTrips.value = emptyList()
                        _upcomingTrips.value = emptyList()
                        _pastTrips.value = emptyList()
                    }
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
package com.datn.apptravel.ui.trip.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class TripsViewModel(
    private val tripRepository: TripRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _trips = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<Trip>> = _trips

    fun getTrips() {
        setLoading(true)

        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    val result = tripRepository.getTripsByUserId(userId)
                    result.onSuccess { tripList ->
                        _trips.value = tripList
                    }.onFailure { exception ->
                        setError(exception.message ?: "Failed to load trips")
                        _trips.value = emptyList()
                    }
                } else {
                    _trips.value = emptyList()
                }
            } catch (e: Exception) {
                setError(e.message ?: "An error occurred")
                _trips.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }
}
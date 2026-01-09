package com.datn.apptravels.ui.discover.profileFollow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.ui.discover.model.DiscoverItem
import com.datn.apptravels.ui.discover.network.ProfileRepository
import kotlinx.coroutines.launch

class ProfileUserViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _trips = MutableLiveData<List<DiscoverItem>>()
    val trips: LiveData<List<DiscoverItem>> = _trips

    fun loadTrips(userId: String, viewerId: String?) {
        viewModelScope.launch {
            try {
                val allTrips = profileRepository.getUserTrips(userId, viewerId)
                // Chỉ hiển thị trips đã được chia sẻ (isPublic != "none")
                _trips.value = allTrips.filter { trip ->
                    trip.isPublic != "none"
                }
            } catch (e: Exception) {
                _trips.value = emptyList()
            }
        }
    }
}




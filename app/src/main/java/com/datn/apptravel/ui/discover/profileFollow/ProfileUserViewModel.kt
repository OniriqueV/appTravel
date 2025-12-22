package com.datn.apptravel.ui.discover.profileFollow

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.ui.discover.model.DiscoverItem
import kotlinx.coroutines.launch

class ProfileUserViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val trips = MutableLiveData<List<DiscoverItem>>()
    val loading = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    fun loadTrips(
        profileUserId: String,
        viewerId: String?
    ) {
        viewModelScope.launch {
            loading.postValue(true)
            try {
                trips.postValue(
                    profileRepository.getUserTrips(profileUserId, viewerId)
                )
            } catch (e: Exception) {
                error.postValue("Không tải được bài viết")
            } finally {
                loading.postValue(false)
            }
        }
    }
}

package com.datn.apptravels.ui.aisuggest.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.ui.aisuggest.data.model.ApiResult
import com.datn.apptravels.ui.aisuggest.data.model.TravelRequest
import com.datn.apptravels.ui.aisuggest.data.repository.TravelRepository
import kotlinx.coroutines.launch
import kotlin.jvm.java

class TravelViewModel(private val repository: TravelRepository) : ViewModel() {

    private val _itineraryResult = MutableLiveData<ApiResult<String>>()
    val itineraryResult: LiveData<ApiResult<String>> = _itineraryResult

    fun generateItinerary(travelRequest: TravelRequest) {
        viewModelScope.launch {
            _itineraryResult.value = ApiResult.Loading
            val result = repository.generateItinerary(travelRequest)
            _itineraryResult.value = result
        }
    }
}

// ViewModel Factory
class TravelViewModelFactory(private val repository: TravelRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TravelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TravelViewModel(repository) as T
        }
        throw kotlin.IllegalArgumentException("Unknown ViewModel class")
    }
}
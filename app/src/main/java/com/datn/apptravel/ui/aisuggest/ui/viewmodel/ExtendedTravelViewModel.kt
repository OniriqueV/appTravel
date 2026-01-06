package com.datn.apptravels.ui.aisuggest.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.ui.aisuggest.data.model.*
import com.datn.apptravels.ui.aisuggest.data.repository.ExtendedTravelRepository
import kotlinx.coroutines.launch

class ExtendedTravelViewModel(private val repository: ExtendedTravelRepository) : ViewModel() {

    // Kết quả tìm kiếm địa điểm
    private val _searchResults = MutableLiveData<ApiResult<List<String>>>()
    val searchResults: LiveData<ApiResult<List<String>>> = _searchResults

    // Lịch trình chính
    private val _itineraryResult = MutableLiveData<ApiResult<String>>()
    val itineraryResult: LiveData<ApiResult<String>> = _itineraryResult

    // Kết quả chat
    private val _chatResult = MutableLiveData<ApiResult<String>>()
    val chatResult: LiveData<ApiResult<String>> = _chatResult

    // Danh sách lịch trình đã lưu
    private val _savedItineraries = MutableLiveData<List<SavedItinerary>>()
    val savedItineraries: LiveData<List<SavedItinerary>> = _savedItineraries

    // Trạng thái lưu
    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    // 1. TÌM KIẾM ĐỊA ĐIỂM
    fun searchPlaces(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            _searchResults.value = ApiResult.Loading
            val result = repository.searchPlaces(query)
            _searchResults.value = result
        }
    }

    // 2. TẠO LỊCH TRÌNH
    fun generateItinerary(travelRequest: TravelRequest) {
        viewModelScope.launch {
            _itineraryResult.value = ApiResult.Loading
            val result = repository.generateItinerary(travelRequest)
            _itineraryResult.value = result
        }
    }

    // 3. CHAT ĐỂ SỬA LỊCH TRÌNH
    fun chatToModify(message: String) {
        viewModelScope.launch {
            _chatResult.value = ApiResult.Loading
            val result = repository.chatToModifyItinerary(message)
            _chatResult.value = result
        }
    }

    // 4. LƯU LỊCH TRÌNH
    fun saveItinerary(
        title: String,
        destination: String,
        days: Int,
        budget: Long,
        people: Int,
        interests: List<String>,
        content: String
    ) {
        viewModelScope.launch {
            val itinerary = SavedItinerary(
                id = System.currentTimeMillis().toString(),
                title = title,
                destination = destination,
                days = days,
                budget = budget,
                people = people,
                interests = interests,
                content = content
            )

            val success = repository.saveItinerary(itinerary)
            _saveStatus.value = success

            if (success) {
                loadSavedItineraries()
            }
        }
    }

    // 5. TẢI DANH SÁCH ĐÃ LƯU
    fun loadSavedItineraries() {
        viewModelScope.launch {
            val list = repository.getSavedItineraries()
            _savedItineraries.value = list
        }
    }

    // 6. XÓA LỊCH TRÌNH
    fun deleteItinerary(id: String) {
        viewModelScope.launch {
            val success = repository.deleteItinerary(id)
            if (success) {
                loadSavedItineraries()
            }
        }
    }

    // 7. LẤY LỊCH SỬ CHAT
    fun getChatHistory(): List<ChatMessage> = repository.getChatHistory()
}

// Factory
class ExtendedTravelViewModelFactory(
    private val repository: ExtendedTravelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExtendedTravelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExtendedTravelViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
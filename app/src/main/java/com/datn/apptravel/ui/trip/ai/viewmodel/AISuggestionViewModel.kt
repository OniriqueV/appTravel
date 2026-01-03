package com.datn.apptravel.ui.trip.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.model.AISuggestedPlan
import com.datn.apptravel.data.model.UserInsight
import com.datn.apptravel.data.repository.AIRepository
import com.datn.apptravel.data.repository.TripRepository
import kotlinx.coroutines.launch

class AISuggestionViewModel(
    private val aiRepository: AIRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val TAG = "AISuggestionViewModel"

    private val _suggestions = MutableLiveData<List<AISuggestedPlan>>()
    val suggestions: LiveData<List<AISuggestedPlan>> = _suggestions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Generate AI suggestions
     */
    fun generateSuggestions(
        tripId: String,
        startDate: String,
        endDate: String,
        userInsight: UserInsight?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                Log.d(TAG, "Starting to generate suggestions for trip: $tripId")

                // Fetch ALL existing plans from backend using the new method
                val existingPlansResult = tripRepository.getAllPlansForTrip(tripId)

                val existingPlans = if (existingPlansResult.isSuccess) {
                    existingPlansResult.getOrNull() ?: emptyList()
                } else {
                    Log.w(TAG, "Failed to fetch existing plans: ${existingPlansResult.exceptionOrNull()?.message}")
                    emptyList()
                }

                Log.d(TAG, "Fetched ${existingPlans.size} existing plans")

                // Call AI repository
                val result = aiRepository.generateAISuggestions(
                    tripId = tripId,
                    startDate = startDate,
                    endDate = endDate,
                    existingPlans = existingPlans,
                    userInsight = userInsight
                )

                if (result.isSuccess) {
                    val suggestedPlans = result.getOrNull() ?: emptyList()
                    _suggestions.value = suggestedPlans
                    Log.d(TAG, "Successfully generated ${suggestedPlans.size} suggestions")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _errorMessage.value = "Không thể tạo gợi ý: $error"
                    Log.e(TAG, "Error generating suggestions: $error")
                }

            } catch (e: Exception) {
                _errorMessage.value = "Đã xảy ra lỗi: ${e.message}"
                Log.e(TAG, "Exception in generateSuggestions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle selection state
     */
    fun toggleSelection(plan: AISuggestedPlan) {
        val currentList = _suggestions.value ?: return
        val updatedList = currentList.map {
            if (it.id == plan.id) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        _suggestions.value = updatedList
    }

    /**
     * Select all suggestions
     */
    fun selectAll() {
        val currentList = _suggestions.value ?: return
        _suggestions.value = currentList.map { it.copy(isSelected = true) }
    }

    /**
     * Deselect all suggestions
     */
    fun deselectAll() {
        val currentList = _suggestions.value ?: return
        _suggestions.value = currentList.map { it.copy(isSelected = false) }
    }

    /**
     * Get selected suggestions
     */
    fun getSelectedSuggestions(): List<AISuggestedPlan> {
        return _suggestions.value?.filter { it.isSelected } ?: emptyList()
    }

    /**
     * Get selection count
     */
    fun getSelectionCount(): Int {
        return _suggestions.value?.count { it.isSelected } ?: 0
    }
}
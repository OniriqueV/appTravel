package com.datn.apptravel.ui.trip.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class PlanDetailViewModel(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    private val _plan = MutableLiveData<Plan>()
    val plan: LiveData<Plan> = _plan

    private val _photos = MutableLiveData<List<String>>()
    val photos: LiveData<List<String>> = _photos

    private val _likesCount = MutableLiveData<Int>()
    val likesCount: LiveData<Int> = _likesCount

    private val _commentsCount = MutableLiveData<Int>()
    val commentsCount: LiveData<Int> = _commentsCount

    private val _isLiked = MutableLiveData<Boolean>()
    val isLiked: LiveData<Boolean> = _isLiked

    private val _uploadSuccess = MutableLiveData<Boolean>()
    val uploadSuccess: LiveData<Boolean> = _uploadSuccess

    private val _commentPosted = MutableLiveData<Boolean>()
    val commentPosted: LiveData<Boolean> = _commentPosted

    init {
        _likesCount.value = 0
        _commentsCount.value = 0
        _isLiked.value = false
        _photos.value = emptyList()
    }

    fun loadPlanPhotos(tripId: String, planId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                Log.d("PlanDetailViewModel", "Loading photos - tripId: $tripId, planId: $planId")
                
                val result = tripRepository.getPlanById(tripId, planId)
                
                result.onSuccess { plan ->
                    _plan.value = plan
                    plan.photos?.let { photosList ->
                        if (photosList.isNotEmpty()) {
                            _photos.value = photosList
                            Log.d("PlanDetailViewModel", "Loaded ${photosList.size} photos")
                        }
                    }
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to load photos", exception)
                    setError("Failed to load photos: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error loading photos", e)
                setError("Error: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    fun uploadPhotos(context: Context, uris: List<Uri>, tripId: String, planId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                Log.d("PlanDetailViewModel", "Uploading ${uris.size} photos...")
                
                val result = tripRepository.uploadImages(context, uris)
                
                result.onSuccess { fileNames ->
                    // Update local photos list
                    val currentPhotos = _photos.value?.toMutableList() ?: mutableListOf()
                    currentPhotos.addAll(fileNames)
                    _photos.value = currentPhotos
                    
                    // Save to backend
                    savePlanPhotos(tripId, planId, currentPhotos)
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Upload failed", exception)
                    setError("Upload failed: ${exception.message}")
                    _uploadSuccess.value = false
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Upload error", e)
                setError("Error: ${e.message}")
                _uploadSuccess.value = false
            } finally {
                setLoading(false)
            }
        }
    }

    private fun savePlanPhotos(tripId: String, planId: String, photos: List<String>) {
        viewModelScope.launch {
            try {
                Log.d("PlanDetailViewModel", "Saving photos to backend...")
                
                val result = tripRepository.updatePlanPhotos(tripId, planId, photos)
                
                result.onSuccess { updatedPlan ->
                    Log.d("PlanDetailViewModel", "Successfully saved ${photos.size} photos")
                    _plan.value = updatedPlan
                    _uploadSuccess.value = true
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to save photos", exception)
                    setError("Failed to save photos: ${exception.message}")
                    _uploadSuccess.value = false
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error saving photos", e)
                setError("Error saving photos: ${e.message}")
                _uploadSuccess.value = false
            }
        }
    }

    fun postComment(comment: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                // TODO: Implement API call to post comment
                Log.d("PlanDetailViewModel", "Posting comment: $comment")
                
                // Simulate success for now
                val currentCount = _commentsCount.value ?: 0
                _commentsCount.value = currentCount + 1
                _commentPosted.value = true
                
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error posting comment", e)
                setError("Error posting comment: ${e.message}")
                _commentPosted.value = false
            } finally {
                setLoading(false)
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            try {
                // TODO: Implement API call to toggle like
                val currentLikes = _likesCount.value ?: 0
                val currentIsLiked = _isLiked.value ?: false
                
                if (currentIsLiked) {
                    _likesCount.value = currentLikes - 1
                    _isLiked.value = false
                } else {
                    _likesCount.value = currentLikes + 1
                    _isLiked.value = true
                }
                
                Log.d("PlanDetailViewModel", "Toggled like: isLiked=${_isLiked.value}, count=${_likesCount.value}")
                
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error toggling like", e)
                setError("Error: ${e.message}")
            }
        }
    }

    fun setInitialCounts(likes: Int, comments: Int) {
        _likesCount.value = likes
        _commentsCount.value = comments
    }

    fun resetUploadSuccess() {
        _uploadSuccess.value = false
    }

    fun resetCommentPosted() {
        _commentPosted.value = false
    }
}
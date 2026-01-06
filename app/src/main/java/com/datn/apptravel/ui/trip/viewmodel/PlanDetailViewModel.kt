package com.datn.apptravels.ui.trip.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.data.model.Plan
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.ui.base.BaseViewModel
import com.datn.apptravels.ui.discover.model.CommentDto
import kotlinx.coroutines.launch

class PlanDetailViewModel(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    private val _plan = MutableLiveData<Plan>()
    val plan: LiveData<Plan> = _plan

    private val _photos = MutableLiveData<List<String>>()
    val photos: LiveData<List<String>> = _photos

    private val _commentsCount = MutableLiveData<Int>()
    val commentsCount: LiveData<Int> = _commentsCount

    private val _comments = MutableLiveData<List<CommentDto>>()
    val comments: LiveData<List<CommentDto>> = _comments

    private val _uploadSuccess = MutableLiveData<Boolean>()
    val uploadSuccess: LiveData<Boolean> = _uploadSuccess

    private val _commentPosted = MutableLiveData<Boolean>()
    val commentPosted: LiveData<Boolean> = _commentPosted
    
    private val _deletePlanSuccess = MutableLiveData<Boolean>()
    val deletePlanSuccess: LiveData<Boolean> = _deletePlanSuccess
    
    private val _deletePhotoSuccess = MutableLiveData<Int>() // photoIndex
    val deletePhotoSuccess: LiveData<Int> = _deletePhotoSuccess
    
    // Store current planId and userId for API calls
    private var currentPlanId: String? = null
    private var currentUserId: String? = null

    init {
        _commentsCount.value = 0
        _photos.value = emptyList()
    }

    fun loadPlanPhotos(tripId: String, planId: String) {
        currentPlanId = planId
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
                    // Load comments after loading plan
                    loadComments()
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

    fun loadComments() {
        viewModelScope.launch {
            try {
                val planId = currentPlanId ?: return@launch
                
                Log.d("PlanDetailViewModel", "Loading comments for planId: $planId")
                
                val result = tripRepository.getComments(planId)
                
                result.onSuccess { commentsList ->
                    _comments.value = commentsList
                    _commentsCount.value = commentsList.size
                    Log.d("PlanDetailViewModel", "Loaded ${commentsList.size} comments")
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to load comments", exception)
                    setError("Failed to load comments: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error loading comments", e)
                setError("Error: ${e.message}")
            }
        }
    }

    fun postComment(comment: String, parentId: String? = null) {
        viewModelScope.launch {
            try {
                val planId = currentPlanId ?: return@launch
                val userId = currentUserId ?: return@launch
                
                Log.d("PlanDetailViewModel", "Posting comment: $comment, parentId: $parentId")
                
                val result = tripRepository.postComment(planId, userId, comment, parentId)
                
                result.onSuccess {
                    Log.d("PlanDetailViewModel", "Comment posted successfully")
                    _commentPosted.value = true
                    // Reload comments after posting
                    loadComments()
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to post comment", exception)
                    setError("Failed to post comment: ${exception.message}")
                    _commentPosted.value = false
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error posting comment", e)
                setError("Error posting comment: ${e.message}")
                _commentPosted.value = false
            }
        }
    }

    fun setInitialCounts(comments: Int) {
        _commentsCount.value = comments
    }
    
    fun setUserId(userId: String) {
        currentUserId = userId
    }

    fun resetUploadSuccess() {
        _uploadSuccess.value = false
    }

    fun resetCommentPosted() {
        _commentPosted.value = false
    }
    
    fun deletePlan(tripId: String, planId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                Log.d("PlanDetailViewModel", "Deleting plan: $planId")
                
                val result = tripRepository.deletePlan(tripId, planId)
                
                result.onSuccess {
                    Log.d("PlanDetailViewModel", "Plan deleted successfully")
                    _deletePlanSuccess.postValue(true)
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to delete plan", exception)
                    setError("Failed to delete plan: ${exception.message}")
                    _deletePlanSuccess.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error deleting plan", e)
                setError("Error: ${e.message}")
                _deletePlanSuccess.postValue(false)
            } finally {
                setLoading(false)
            }
        }
    }
    
    fun deletePhoto(tripId: String, planId: String, photoFileName: String, photoIndex: Int) {
        viewModelScope.launch {
            try {
                setLoading(true)
                Log.d("PlanDetailViewModel", "Deleting photo: $photoFileName")
                
                val result = tripRepository.deletePhotoFromPlan(tripId, planId, photoFileName)
                
                result.onSuccess {
                    Log.d("PlanDetailViewModel", "Photo deleted successfully")
                    // Remove from local list
                    val currentPhotos = _photos.value?.toMutableList() ?: mutableListOf()
                    if (photoIndex < currentPhotos.size) {
                        currentPhotos.removeAt(photoIndex)
                        _photos.postValue(currentPhotos)
                    }
                    _deletePhotoSuccess.postValue(photoIndex)
                }.onFailure { exception ->
                    Log.e("PlanDetailViewModel", "Failed to delete photo", exception)
                    setError("Failed to delete photo: ${exception.message}")
                    _deletePhotoSuccess.postValue(-1)
                }
            } catch (e: Exception) {
                Log.e("PlanDetailViewModel", "Error deleting photo", e)
                setError("Error: ${e.message}")
                _deletePhotoSuccess.postValue(-1)
            } finally {
                setLoading(false)
            }
        }
    }
}
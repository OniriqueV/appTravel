package com.datn.apptravel.ui.trip.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.model.request.CreateTripRequest
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class CreateTripViewModel(
    private val tripRepository: TripRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {
    
    // Create trip result
    private val _createTripResult = MutableLiveData<Trip?>()
    val createTripResult: LiveData<Trip?> = _createTripResult
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Store trip data while uploading
    private var pendingTripId: String? = null
    private var pendingTripTitle: String? = null
    private var pendingStartDate: String? = null
    private var pendingEndDate: String? = null
    
    fun uploadCoverPhoto(context: Context, imageUri: Uri) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                val result = tripRepository.uploadImage(context, imageUri)
                
                result.onSuccess { fileName ->
                    // Create trip with uploaded photo
                    createTripWithCoverPhoto(fileName)
                }.onFailure { exception ->
                    _errorMessage.value = "Upload failed: ${exception.message}"
                    setLoading(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Upload error: ${e.message}"
                setLoading(false)
            }
        }
    }
    
    private fun createTripWithCoverPhoto(coverPhotoFileName: String) {
        if (pendingTripTitle != null && pendingStartDate != null && pendingEndDate != null) {
            if (pendingTripId != null) {
                // Update existing trip with new photo
                updateTrip(
                    tripId = pendingTripId!!,
                    title = pendingTripTitle!!,
                    startDate = pendingStartDate!!,
                    endDate = pendingEndDate!!,
                    coverPhotoUri = coverPhotoFileName
                )
            } else {
                // Create new trip
                createTrip(
                    title = pendingTripTitle!!,
                    startDate = pendingStartDate!!,
                    endDate = pendingEndDate!!,
                    coverPhotoUri = coverPhotoFileName
                )
            }
        }
    }
    
    fun setPendingTripData(title: String, startDate: String, endDate: String, tripId: String? = null) {
        pendingTripId = tripId
        pendingTripTitle = title
        pendingStartDate = startDate
        pendingEndDate = endDate
    }

    fun createTrip(
        title: String, 
        startDate: String, 
        endDate: String,
        coverPhotoUri: String? = null
    ) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                // Get current user ID from session (Firebase UID)
                val userId = sessionManager.getUserId() ?: "anonymous" // Default if not logged in
                
                // Convert date format from dd/MM/yyyy to yyyy-MM-dd
                val formattedStartDate = convertDateFormat(startDate)
                val formattedEndDate = convertDateFormat(endDate)
                
                val request = CreateTripRequest(
                    userId = userId,
                    title = title,
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    isPublic = "none",
                    coverPhoto = coverPhotoUri,
                    content = null,
                    tags = null,
                    sharedAt = null
                )
                
                val result = tripRepository.createTrip(request)
                
                result.onSuccess { trip ->
                    _createTripResult.value = trip
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Failed to create trip"
                    _createTripResult.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
                _createTripResult.value = null
            } finally {
                setLoading(false)
            }
        }
    }
    
    fun updateTrip(
        tripId: String,
        title: String,
        startDate: String,
        endDate: String,
        coverPhotoUri: String? = null,
        isPublic: String = "none",
        content: String? = null,
        tags: String? = null,
        sharedAt: String? = null
    ) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                // Get current user ID from session
                val userId = sessionManager.getUserId() ?: "anonymous"
                
                // Convert date format from dd/MM/yyyy to yyyy-MM-dd
                val formattedStartDate = convertDateFormat(startDate)
                val formattedEndDate = convertDateFormat(endDate)
                
                val request = CreateTripRequest(
                    userId = userId,
                    title = title,
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    isPublic = isPublic,
                    coverPhoto = coverPhotoUri,
                    content = content,
                    tags = tags,
                    sharedAt = sharedAt
                )
                
                val result = tripRepository.updateTrip(tripId, request)
                
                result.onSuccess { trip ->
                    _createTripResult.value = trip
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Failed to update trip"
                    _createTripResult.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
                _createTripResult.value = null
            } finally {
                setLoading(false)
            }
        }
    }
    
    private fun convertDateFormat(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString // Return original if conversion fails
        }
    }

}
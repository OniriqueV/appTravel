package com.datn.apptravels.ui.trip.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.model.User
import com.datn.apptravels.data.model.request.CreateTripRequest
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    
    private val _dateConflictTrips = MutableLiveData<List<Trip>>()
    val dateConflictTrips: LiveData<List<Trip>> = _dateConflictTrips
    
    // Store trip data while uploading
    private var pendingTripId: String? = null
    private var pendingTripTitle: String? = null
    private var pendingStartDate: String? = null
    private var pendingEndDate: String? = null
    private var pendingMembers: List<User>? = null
    
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
                    coverPhotoUri = coverPhotoFileName,
                    members = pendingMembers
                )
            } else {
                // Create new trip with members
                createTrip(
                    title = pendingTripTitle!!,
                    startDate = pendingStartDate!!,
                    endDate = pendingEndDate!!,
                    coverPhotoUri = coverPhotoFileName,
                    members = pendingMembers
                )
            }
        }
    }
    
    fun setPendingTripData(title: String, startDate: String, endDate: String, tripId: String? = null, members: List<User>? = null) {
        pendingTripId = tripId
        pendingTripTitle = title
        pendingStartDate = startDate
        pendingEndDate = endDate
        pendingMembers = members
    }

    fun createTrip(
        title: String, 
        startDate: String, 
        endDate: String,
        coverPhotoUri: String? = null,
        members: List<User>? = null
    ) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                // Get current user ID from session (Firebase UID)
                val userId = sessionManager.getUserId() ?: "anonymous" // Default if not logged in
                
                // Convert date format from dd/MM/yyyy to yyyy-MM-dd
                val formattedStartDate = convertDateFormat(startDate)
                val formattedEndDate = convertDateFormat(endDate)
                
                // Validate that end date is after start date
                if (formattedEndDate <= formattedStartDate) {
                    _errorMessage.value = "Ngày kết thúc phải sau ngày bắt đầu"
                    setLoading(false)
                    return@launch
                }
                
                // Check for date conflicts with existing trips
                val hasConflict = checkDateConflict(formattedStartDate, formattedEndDate, null)
                if (hasConflict) {
                    setLoading(false)
                    return@launch // Stop if there's a conflict
                }
                
                val request = CreateTripRequest(
                    userId = userId,
                    title = title,
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    isPublic = "none",
                    coverPhoto = coverPhotoUri,
                    content = null,
                    tags = null,
                    members = members,
                    sharedWithUsers = null,
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
        isPublic: String? = null,
        content: String? = null,
        tags: String? = null,
        sharedAt: String? = null,
        members: List<User>? = null
    ) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                // Get current user ID from session
                val userId = sessionManager.getUserId() ?: "anonymous"
                
                // Convert date format from dd/MM/yyyy to yyyy-MM-dd
                val formattedStartDate = convertDateFormat(startDate)
                val formattedEndDate = convertDateFormat(endDate)
                
                // Validate that end date is after start date
                if (formattedEndDate <= formattedStartDate) {
                    _errorMessage.value = "Ngày kết thúc phải sau ngày bắt đầu"
                    setLoading(false)
                    return@launch
                }
                
                // Check if plans are within the new date range
                val plansOutOfRange = checkPlansWithinDateRange(tripId, formattedStartDate, formattedEndDate)
                if (plansOutOfRange) {
                    setLoading(false)
                    return@launch // Stop if there are plans outside the new date range
                }
                
                // Check for date conflicts with existing trips (excluding current trip)
                val hasConflict = checkDateConflict(formattedStartDate, formattedEndDate, tripId)
                if (hasConflict) {
                    setLoading(false)
                    return@launch // Stop if there's a conflict
                }
                
                // Fetch existing trip to preserve sharing settings
                val existingTripResult = tripRepository.getTripById(tripId)
                var existingTrip: Trip? = null
                existingTripResult.onSuccess { trip ->
                    existingTrip = trip
                }
                
                val request = CreateTripRequest(
                    userId = userId,
                    title = title,
                    startDate = formattedStartDate,
                    endDate = formattedEndDate,
                    isPublic = isPublic ?: existingTrip?.isPublic ?: "none", // Preserve existing isPublic if not provided
                    coverPhoto = coverPhotoUri,
                    content = content ?: existingTrip?.content, // Preserve existing content if not provided
                    tags = tags ?: existingTrip?.tags, // Preserve existing tags if not provided
                    members = members,
                    sharedWithUsers = existingTrip?.sharedWithUsers, // Always preserve sharedWithUsers
                    sharedAt = sharedAt ?: existingTrip?.sharedAt // Preserve existing sharedAt if not provided
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

    private suspend fun checkDateConflict(
        startDate: String,
        endDate: String,
        excludeTripId: String?
    ): Boolean {
        try {
            val userId = sessionManager.getUserId() ?: return false
            
            // Fetch both owned trips and member trips
            val ownTripsResult = tripRepository.getTripsByUserId(userId)
            val memberTripsResult = tripRepository.getTripsByMemberId(userId)
            
            val ownTrips = ownTripsResult.getOrNull() ?: emptyList()
            val memberTrips = memberTripsResult.getOrNull() ?: emptyList()
            
            // Merge both lists to check conflicts against all trips
            val allTrips = (ownTrips + memberTrips).distinctBy { it.id }
            
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val newStartDate = LocalDate.parse(startDate, dateFormatter)
            val newEndDate = LocalDate.parse(endDate, dateFormatter)
            
            val conflictingTrips = allTrips.filter { trip ->
                // Exclude the current trip when updating
                if (excludeTripId != null && trip.id == excludeTripId) {
                    return@filter false
                }
                
                try {
                    val existingStartDate = LocalDate.parse(trip.startDate, dateFormatter)
                    val existingEndDate = LocalDate.parse(trip.endDate, dateFormatter)
                    
                    // Check if date ranges overlap
                    // Two date ranges overlap if:
                    // new start <= existing end AND new end >= existing start
                    !(newEndDate.isBefore(existingStartDate) || newStartDate.isAfter(existingEndDate))
                } catch (e: Exception) {
                    false
                }
            }
            
            if (conflictingTrips.isNotEmpty()) {
                _dateConflictTrips.value = conflictingTrips
                val tripTitles = conflictingTrips.joinToString(", ") { "\"${it.title}\"" }
                _errorMessage.value = "Thời gian bị trùng với chuyến đi: $tripTitles"
                return true
            }
            
            // Check for failures in API calls
            if (ownTripsResult.isFailure) {
                _errorMessage.value = "Không thể kiểm tra trùng lặp: ${ownTripsResult.exceptionOrNull()?.message}"
                return true
            }
            
            return false
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi kiểm tra trùng lặp: ${e.message}"
            return true
        }
    }

    private suspend fun checkPlansWithinDateRange(
        tripId: String,
        startDate: String,
        endDate: String
    ): Boolean {
        try {
            val result = tripRepository.getTripById(tripId)
            
            result.onSuccess { trip ->
                val plans = trip.plans ?: emptyList()
                
                if (plans.isEmpty()) {
                    return false // No plans to check
                }
                
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val newStartDate = LocalDate.parse(startDate, dateFormatter)
                val newEndDate = LocalDate.parse(endDate, dateFormatter)
                
                // Check each plan's date
                val plansOutOfRange = plans.filter { plan ->
                    try {
                        // Extract date from startTime format: yyyy-MM-dd'T'HH:mm:ss
                        val planDateStr = plan.startTime.substring(0, 10) // Get yyyy-MM-dd part
                        val planDate = LocalDate.parse(planDateStr, dateFormatter)
                        
                        // Check if plan date is outside the new date range
                        planDate.isBefore(newStartDate) || planDate.isAfter(newEndDate)
                    } catch (e: Exception) {
                        false // If can't parse date, assume it's valid
                    }
                }
                
                if (plansOutOfRange.isNotEmpty()) {
                    val planTitles = plansOutOfRange.joinToString(", ") { "\"${it.title}\"" }
                    val count = plansOutOfRange.size
                    _errorMessage.value = "Không thể thay đổi thời gian! Có $count kế hoạch nằm ngoài khoảng thời gian mới: $planTitles. Vui lòng xóa hoặc điều chỉnh các kế hoạch này trước."
                    return true
                }
            }.onFailure { exception ->
                _errorMessage.value = "Không thể kiểm tra kế hoạch: ${exception.message}"
                return true
            }
            
            return false
        } catch (e: Exception) {
            _errorMessage.value = "Lỗi khi kiểm tra kế hoạch: ${e.message}"
            return true
        }
    }

}
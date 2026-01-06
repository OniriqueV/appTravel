package com.datn.apptravels.ui.trip.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.R
import com.datn.apptravels.data.model.AISuggestedPlan
import com.datn.apptravels.data.model.CityPlan
import com.datn.apptravels.data.model.Plan
import com.datn.apptravels.data.model.PlanType
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.model.User
import com.datn.apptravels.data.model.request.CreateActivityPlanRequest
import com.datn.apptravels.data.model.request.CreateBoatPlanRequest
import com.datn.apptravels.data.model.request.CreateCarRentalPlanRequest
import com.datn.apptravels.data.model.request.CreateFlightPlanRequest
import com.datn.apptravels.data.model.request.CreateLodgingPlanRequest
import com.datn.apptravels.data.model.request.CreateRestaurantPlanRequest
import com.datn.apptravels.data.model.request.CreateTripRequest
import com.datn.apptravels.data.repository.AIRepository
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.ui.trip.model.ScheduleActivity
import com.datn.apptravels.ui.trip.model.ScheduleDay
import com.datn.apptravels.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TripDetailViewModel(
    private val tripRepository: TripRepository,
    private val aiRepository: AIRepository
) : BaseViewModel() {
    
    // Trip details
    private val _tripDetails = MutableLiveData<Trip?>()
    val tripDetails: LiveData<Trip?> = _tripDetails
    
    // Schedule days
    private val _scheduleDays = MutableLiveData<List<ScheduleDay>>()
    val scheduleDays: LiveData<List<ScheduleDay>> = _scheduleDays
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Processed data for UI
    private val _displayMembers = MutableLiveData<List<User>>()
    val displayMembers: LiveData<List<User>> = _displayMembers
    
    private val _planButtonText = MutableLiveData<String>()
    val planButtonText: LiveData<String> = _planButtonText
    
    private val _isTripEnded = MutableLiveData<Boolean>()
    val isTripEnded: LiveData<Boolean> = _isTripEnded
    
    private val _canViewMap = MutableLiveData<Boolean>()
    val canViewMap: LiveData<Boolean> = _canViewMap
    
    private val _showShareButton = MutableLiveData<Boolean>()
    val showShareButton: LiveData<Boolean> = _showShareButton
    
    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess
    
    private val _shareSuccess = MutableLiveData<Boolean>()
    val shareSuccess: LiveData<Boolean> = _shareSuccess
    
    private val _followers = MutableLiveData<List<User>>()
    val followers: LiveData<List<User>> = _followers
    
    // Ownership/membership status exposed to UI
    private val _isOwner = MutableLiveData<Boolean>()
    val isOwner: LiveData<Boolean> = _isOwner
    
    private val _isMember = MutableLiveData<Boolean>()
    val isMember: LiveData<Boolean> = _isMember
    
    // Current user info
    private var currentUserId: String? = null
    
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        // Recalculate permissions if trip is already loaded
        _tripDetails.value?.let { trip ->
            calculatePermissions(trip)
        }
    }
    
    private fun calculatePermissions(trip: Trip) {
        val userId = currentUserId ?: return
        
        val owner = trip.userId == userId
        val member = trip.members?.any { it.id == userId } == true
        
        _isOwner.value = owner
        _isMember.value = member
        
        // Update UI states based on permissions
        _showShareButton.value = owner
        updateTripEndedStatus(trip)
        updateDisplayMembers(trip)
        
        Log.d("TripDetailViewModel", "Permissions - isOwner: $owner, isMember: $member")
    }

    fun getTripDetails(tripId: String) {
        setLoading(true)
        
        viewModelScope.launch {
            try {
                if (tripId.isBlank()) {
                    _errorMessage.value = "Invalid trip ID"
                    setLoading(false)
                    return@launch
                }
                
                // Load trip details
                val tripResult = tripRepository.getTripById(tripId)
                tripResult.onSuccess { trip ->
                    _tripDetails.value = trip
                    calculatePermissions(trip)
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Failed to load trip"
                    _tripDetails.value = null
                }
                
                // Load plans separately
                val plansResult = tripRepository.getPlansByTripId(tripId)
                plansResult.onSuccess { plans ->
                    generateScheduleDaysFromPlans(plans)
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Failed to load plans"
                    _scheduleDays.value = emptyList()
                }
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
                _tripDetails.value = null
                _scheduleDays.value = emptyList()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun generateScheduleDaysFromPlans(plans: List<Plan>) {
        if (plans.isEmpty()) {
            _scheduleDays.value = emptyList()
            return
        }
        
        // Group plans by date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val plansByDate = plans.groupBy { plan ->
            try {
                // Parse ISO datetime and extract date
                val dateTime = LocalDateTime.parse(
                    plan.startTime,
                    DateTimeFormatter.ISO_DATE_TIME
                )
                dateTime.toLocalDate().toString()
            } catch (e: Exception) {
                "Unknown"
            }
        }
        
        // Create ScheduleDay for each date
        val scheduleDaysList = plansByDate.map { (dateString, plansForDay) ->
            val activities = plansForDay.map { plan ->
                val startTime = try {
                    val dateTime = LocalDateTime.parse(
                        plan.startTime,
                        DateTimeFormatter.ISO_DATE_TIME
                    )
                    String.format("%02d:%02d", dateTime.hour, dateTime.minute)
                } catch (e: Exception) {
                    "00:00"
                }
                
                ScheduleActivity(
                    id = plan.id,
                    tripId = plan.tripId,
                    time = startTime,
                    title = plan.title,
                    description = plan.address ?: "",
                    location = plan.address ?: "",
                    type = plan.type,
                    expense = plan.expense,
                    iconResId = getIconForPlanType(plan.type),
                    fullStartTime = plan.startTime,
                    endTime = plan.endTime,
                    checkInDate = plan.checkInDate,
                    checkOutDate = plan.checkOutDate,
                    arrivalDate = plan.arrivalDate,
                    arrivalTime = plan.arrivalTime,
                    reservationDate = plan.reservationDate,
                    reservationTime = plan.reservationTime
                )
            }
            
            val displayDate = try {
                val date = LocalDate.parse(dateString)
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } catch (e: Exception) {
                dateString
            }
            
            ScheduleDay(
                dayNumber = 0,
                title = displayDate,
                date = displayDate,
                activities = activities
            )
        }.sortedBy { scheduleDay ->
            // Sort by date
            try {
                val date = LocalDate.parse(
                    scheduleDay.date,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
                date.toString()
            } catch (e: Exception) {
                scheduleDay.date
            }
        }
        
        _scheduleDays.value = scheduleDaysList
    }
    
    private fun getIconForPlanType(planType: PlanType): Int {
        return when (planType) {
            PlanType.LODGING -> R.drawable.ic_lodgingsss
            PlanType.RESTAURANT -> R.drawable.ic_restaurant
            PlanType.FLIGHT -> R.drawable.ic_flight
            PlanType.CAR_RENTAL -> R.drawable.ic_car
            PlanType.TRAIN -> R.drawable.ic_train
            PlanType.BOAT -> R.drawable.ic_boat
            PlanType.TOUR -> R.drawable.ic_toursss
            PlanType.ACTIVITY -> R.drawable.ic_attraction
            PlanType.THEATER -> R.drawable.ic_theater
            PlanType.SHOPPING -> R.drawable.ic_shopping
            PlanType.CAMPING -> R.drawable.ic_location
            PlanType.RELIGION -> R.drawable.ic_religion
            PlanType.NONE -> R.drawable.ic_globe
        }
    }

    private fun generateScheduleDaysFromTrip(trip: Trip) {
        if (trip.plans.isNullOrEmpty()) {
            _scheduleDays.value = emptyList()
            return
        }

        // Use the plans from trip object
        generateScheduleDaysFromPlans(trip.plans)
    }

    // AI Plan Generation
    private val _aiGenerationStatus = MutableLiveData<AIGenerationStatus>()
    val aiGenerationStatus: LiveData<AIGenerationStatus> = _aiGenerationStatus

    private val _aiGeneratedPlans = MutableLiveData<List<AISuggestedPlan>>()
    val aiGeneratedPlans: LiveData<List<AISuggestedPlan>> = _aiGeneratedPlans

    sealed class AIGenerationStatus {
        object Idle : AIGenerationStatus()
        object Loading : AIGenerationStatus()
        data class Success(val plansCreated: Int) : AIGenerationStatus()
        data class Error(val message: String) : AIGenerationStatus()
    }

    fun generateAIPlansForCities(cities: List<CityPlan>) {
        viewModelScope.launch {
            try {
                _aiGenerationStatus.value = AIGenerationStatus.Loading
                Log.d("TripDetailViewModel", "Generating AI plans for ${cities.size} cities")

                // Generate AI suggestions for all cities
                val suggestionsResult = aiRepository.generatePlansForMultipleCities(cities)

                if (suggestionsResult.isFailure) {
                    _aiGenerationStatus.value = AIGenerationStatus.Error(
                        suggestionsResult.exceptionOrNull()?.message ?: "Không thể tạo kế hoạch"
                    )
                    return@launch
                }

                val suggestions = suggestionsResult.getOrNull() ?: emptyList()
                Log.d("TripDetailViewModel", "Generated ${suggestions.size} AI suggestions")

                if (suggestions.isEmpty()) {
                    _aiGenerationStatus.value = AIGenerationStatus.Error("AI không tạo được kế hoạch nào")
                    return@launch
                }

                // Emit suggestions to show preview dialog
                _aiGeneratedPlans.value = suggestions
                _aiGenerationStatus.value = AIGenerationStatus.Idle

            } catch (e: Exception) {
                Log.e("TripDetailViewModel", "Error in AI generation", e)
                _aiGenerationStatus.value = AIGenerationStatus.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun saveAIPlans(context: Context, tripId: String, plans: List<AISuggestedPlan>) {
        viewModelScope.launch {
            try {
                _aiGenerationStatus.value = AIGenerationStatus.Loading
                Log.d("TripDetailViewModel", "Saving ${plans.size} AI plans")

                // Create plans via API
                var successCount = 0
                plans.forEach { suggestion ->
                    val result = createPlanFromSuggestion(context, tripId, suggestion)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        Log.e("TripDetailViewModel", "Failed to create plan: ${suggestion.title}")
                    }
                }

                if (successCount > 0) {
                    _aiGenerationStatus.value = AIGenerationStatus.Success(successCount)
                    // Reload trip data to show new plans
                    getTripDetails(tripId)
                } else {
                    _aiGenerationStatus.value = AIGenerationStatus.Error("Không thể lưu kế hoạch vào hệ thống")
                }

            } catch (e: Exception) {
                Log.e("TripDetailViewModel", "Error saving AI plans", e)
                _aiGenerationStatus.value = AIGenerationStatus.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }


    private suspend fun createPlanFromSuggestion(context: Context, tripId: String, suggestion: AISuggestedPlan): Result<Plan> {
        // Download and upload image if photoUrl is provided
        var uploadedFilename: String? = null
        
        Log.d("TripDetailViewModel", "=== Processing plan: ${suggestion.title} ===")
        Log.d("TripDetailViewModel", "photoUrl value: '${suggestion.photoUrl}'")
        Log.d("TripDetailViewModel", "photoUrl isNullOrEmpty: ${suggestion.photoUrl.isNullOrEmpty()}")
        Log.d("TripDetailViewModel", "photoUrl startsWith http: ${suggestion.photoUrl?.startsWith("http")}")
        
        if (!suggestion.photoUrl.isNullOrEmpty() && suggestion.photoUrl.startsWith("http")) {
            Log.d("TripDetailViewModel", "✓ Valid URL detected! Downloading and uploading image for: ${suggestion.title}")
            val uploadResult = tripRepository.downloadAndUploadImage(context, suggestion.photoUrl)
            uploadResult.onSuccess { filename ->
                uploadedFilename = filename
                Log.d("TripDetailViewModel", "✓ Image uploaded successfully: $filename")
            }.onFailure { exception ->
                Log.e("TripDetailViewModel", "✗ Failed to upload image: ${exception.message}", exception)
            }
        } else {
            uploadedFilename = suggestion.photoUrl
            Log.d("TripDetailViewModel", "✗ No valid photoUrl to upload (either null/empty or doesn't start with http)")
        }

        return when (suggestion.type) {
            PlanType.ACTIVITY, PlanType.TOUR, PlanType.THEATER, PlanType.SHOPPING, PlanType.CAMPING, PlanType.RELIGION -> {
                val request = CreateActivityPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 2), // Default 2 hours duration
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = suggestion.type.name
                )
                tripRepository.createActivityPlan(tripId, request)
            }

            PlanType.RESTAURANT -> {
                val request = CreateRestaurantPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 1), // Default 1 hour duration
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "RESTAURANT"
                )
                tripRepository.createRestaurantPlan(tripId, request)
            }

            PlanType.LODGING -> {
                val request = CreateLodgingPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 24), // Default 1 day
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "LODGING",
                    checkInDate = suggestion.startTime,
                    checkOutDate = calculateEndTime(suggestion.startTime, 24)
                )
                tripRepository.createLodgingPlan(tripId, request)
            }

            PlanType.FLIGHT -> {
                val request = CreateFlightPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 3), // Default 3 hours
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "FLIGHT"
                )
                tripRepository.createFlightPlan(tripId, request)
            }

            PlanType.TRAIN -> {
                val request = CreateActivityPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 4), // Default 4 hours
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "TRAIN"
                )
                tripRepository.createActivityPlan(tripId, request)
            }

            PlanType.BOAT -> {
                val request = CreateBoatPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 2), // Default 2 hours
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "BOAT"
                )
                tripRepository.createBoatPlan(tripId, request)
            }

            PlanType.CAR_RENTAL -> {
                val request = CreateCarRentalPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 8), // Default 8 hours
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "CAR_RENTAL"
                )
                tripRepository.createCarRentalPlan(tripId, request)
            }

            else -> {
                // For other types, create as generic activity
                val request = CreateActivityPlanRequest(
                    tripId = tripId,
                    title = suggestion.title,
                    address = suggestion.address,
                    location = "${suggestion.lat},${suggestion.lng}",
                    startTime = suggestion.startTime,
                    endTime = calculateEndTime(suggestion.startTime, 2),
                    expense = suggestion.expense,
                    photoUrl = uploadedFilename,
                    type = "ACTIVITY"
                )
                tripRepository.createActivityPlan(tripId, request)
            }
        }
    }

    private fun calculateEndTime(startTime: String, hoursToAdd: Int): String {
        return try {
            val dateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME)
            val endDateTime = dateTime.plusHours(hoursToAdd.toLong())
            endDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            startTime // Return same time if parsing fails
        }
    }

    fun resetAIGenerationStatus() {
        _aiGenerationStatus.value = AIGenerationStatus.Idle
    }
    
    private fun updateTripEndedStatus(trip: Trip) {
        val ended = try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val endDate = LocalDate.parse(trip.endDate, dateFormatter)
            val today = LocalDate.now()
            endDate.isBefore(today)
        } catch (e: Exception) {
            false
        }
        
        _isTripEnded.value = ended
        _canViewMap.value = !ended
        _planButtonText.value = if (ended) "View Map" else "Add new plan"
    }
    
    private fun updateDisplayMembers(trip: Trip) {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val owner = _isOwner.value ?: false
                val member = _isMember.value ?: false
                val displayList = mutableListOf<User>()
                
                if (owner) {
                    // Owner: Show all members except self
                    trip.members?.forEach { m ->
                        if (m.id != userId) {
                            displayList.add(m)
                        }
                    }
                } else if (member) {
                    // Member: Show owner first, then other members except self
                    val ownerResult = tripRepository.getUserById(trip.userId)
                    ownerResult.onSuccess { owner ->
                        displayList.add(owner)
                    }
                    
                    trip.members?.forEach { m ->
                        if (m.id != userId) {
                            displayList.add(m)
                        }
                    }
                }
                
                _displayMembers.postValue(displayList)
                Log.d("TripDetailViewModel", "Display members: ${displayList.size}")
            } catch (e: Exception) {
                Log.e("TripDetailViewModel", "Error updating display members", e)
                _displayMembers.postValue(emptyList())
            }
        }
    }
    
    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val result = tripRepository.deleteTrip(tripId)
                
                result.onSuccess {
                    _deleteSuccess.postValue(true)
                    Log.d("TripDetailViewModel", "Trip deleted successfully")
                }.onFailure { exception ->
                    _errorMessage.postValue(exception.message ?: "Failed to delete trip")
                    _deleteSuccess.postValue(false)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Error deleting trip")
                _deleteSuccess.postValue(false)
            } finally {
                setLoading(false)
            }
        }
    }
    
    fun shareTrip(
        tripId: String,
        userId: String,
        title: String,
        startDate: String,
        endDate: String,
        coverPhoto: String?,
        content: String,
        tags: String,
        isPublic: String,
        sharedAt: String?,
        members: List<User>?,
        sharedWithUsers: List<User>?
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val updateRequest = CreateTripRequest(
                    userId = userId,
                    title = title,
                    startDate = startDate,
                    endDate = endDate,
                    isPublic = isPublic,
                    coverPhoto = coverPhoto,
                    content = content,
                    tags = tags,
                    members = members,
                    sharedWithUsers = sharedWithUsers,
                    sharedAt = sharedAt
                )
                
                val result = tripRepository.updateTrip(tripId, updateRequest)
                
                result.onSuccess {
                    _shareSuccess.postValue(true)
                    // Reload trip to get updated data
                    getTripDetails(tripId)
                }.onFailure { exception ->
                    _errorMessage.postValue(exception.message ?: "Failed to share trip")
                    _shareSuccess.postValue(false)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Error sharing trip")
                _shareSuccess.postValue(false)
            } finally {
                setLoading(false)
            }
        }
    }
    
    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            try {
                val result = tripRepository.getFollowers(userId)
                
                result.onSuccess { followersList ->
                    _followers.postValue(followersList)
                }.onFailure { exception ->
                    _errorMessage.postValue(exception.message ?: "Failed to load followers")
                    _followers.postValue(emptyList())
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "Error loading followers")
                _followers.postValue(emptyList())
            }
        }
    }
}
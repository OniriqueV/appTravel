package com.datn.apptravel.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.model.ScheduleActivity
import com.datn.apptravel.ui.model.ScheduleDay
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TripDetailViewModel(private val tripRepository: TripRepository) : BaseViewModel() {
    
    // Trip details
    private val _tripDetails = MutableLiveData<Trip?>()
    val tripDetails: LiveData<Trip?> = _tripDetails
    
    // Schedule days
    private val _scheduleDays = MutableLiveData<List<ScheduleDay>>()
    val scheduleDays: LiveData<List<ScheduleDay>> = _scheduleDays
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

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

    private fun generateScheduleDaysFromPlans(plans: List<com.datn.apptravel.data.model.Plan>) {
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
                val dateTime = java.time.LocalDateTime.parse(
                    plan.startTime,
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME
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
                    val dateTime = java.time.LocalDateTime.parse(
                        plan.startTime,
                        java.time.format.DateTimeFormatter.ISO_DATE_TIME
                    )
                    String.format("%02d:%02d", dateTime.hour, dateTime.minute)
                } catch (e: Exception) {
                    "00:00"
                }
                
                ScheduleActivity(
                    time = startTime,
                    title = plan.title,
                    description = plan.address ?: "",
                    location = plan.address ?: "",
                    type = plan.type,
                    iconResId = getIconForPlanType(plan.type)
                )
            }
            
            val displayDate = try {
                val date = java.time.LocalDate.parse(dateString)
                date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
                val date = java.time.LocalDate.parse(
                    scheduleDay.date,
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
                date.toString()
            } catch (e: Exception) {
                scheduleDay.date
            }
        }
        
        _scheduleDays.value = scheduleDaysList
    }
    
    private fun getIconForPlanType(planType: com.datn.apptravel.data.model.PlanType): Int {
        return when (planType) {
            com.datn.apptravel.data.model.PlanType.RESTAURANT -> com.datn.apptravel.R.drawable.ic_restaurant
            com.datn.apptravel.data.model.PlanType.LODGING -> com.datn.apptravel.R.drawable.ic_lodging
            com.datn.apptravel.data.model.PlanType.FLIGHT -> com.datn.apptravel.R.drawable.ic_flight
            com.datn.apptravel.data.model.PlanType.BOAT -> com.datn.apptravel.R.drawable.ic_boat
            com.datn.apptravel.data.model.PlanType.CAR_RENTAL -> com.datn.apptravel.R.drawable.ic_car
            com.datn.apptravel.data.model.PlanType.ACTIVITY -> com.datn.apptravel.R.drawable.ic_attraction
            else -> com.datn.apptravel.R.drawable.ic_location
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

    fun updateTripDetails(tripId: String, updatedDetails: Map<String, Any>) {
        setLoading(true)
        
        // Simulated API call with delay
        android.os.Handler().postDelayed({
            // Simulate updating trip details
            val trip = _tripDetails.value
            _tripDetails.value = trip // Just post the same value for now
            setLoading(false)
        }, 1000)
    }
}
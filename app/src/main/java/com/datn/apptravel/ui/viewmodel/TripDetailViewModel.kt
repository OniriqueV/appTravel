package com.datn.apptravel.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.datn.apptravel.ui.model.ScheduleActivity
import com.datn.apptravel.ui.model.ScheduleDay
import com.datn.apptravel.ui.activity.MainActivity
import com.datn.apptravel.ui.base.BaseViewModel
import com.datn.apptravel.util.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TripDetailViewModel : BaseViewModel() {
    
    // Trip details
    private val _tripDetails = MutableLiveData<Trip?>()
    val tripDetails: LiveData<Trip?> = _tripDetails
    
    // Schedule days
    private val _scheduleDays = MutableLiveData<List<ScheduleDay>>()
    val scheduleDays: LiveData<List<ScheduleDay>> = _scheduleDays

    fun getTripDetails(tripId: String) {
        setLoading(true)
        
        // Simulated API call with delay
        android.os.Handler().postDelayed({
            // Get trip from TripManager
            val allTrips = MainActivity.tripManager.getAllTrips()
            val trip = allTrips.find { it.id == tripId } ?: allTrips.firstOrNull()
            
            _tripDetails.value = trip
            
            // Also generate some sample schedule days for this trip
            generateSampleScheduleDays(trip)
            
            setLoading(false)
        }, 1000)
    }

    private fun generateSampleScheduleDays(trip: Trip?) {
        if (trip == null) {
            _scheduleDays.value = emptyList()
            return
        }
        
        // Parse start date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = try {
            dateFormat.parse(trip.startDate)
        } catch (e: Exception) {
            Calendar.getInstance().time
        }
        
        // Generate schedule days
        val scheduleDaysList = mutableListOf<ScheduleDay>()
        
        // Day 1
        val day1 = ScheduleDay(
            dayNumber = 1,
            title = "Arrival in ${trip.destination.split(",").firstOrNull() ?: trip.destination}",
            date = trip.startDate,
            activities = listOf(
                ScheduleActivity(
                    time = "10:00 AM",
                    title = "Arrival at Airport",
                    description = "Flight lands at main airport"
                ),
                ScheduleActivity(
                    time = "12:00 PM",
                    title = "Check-in at Hotel",
                    description = "Hotel ${trip.destination}"
                ),
                ScheduleActivity(
                    time = "2:00 PM",
                    title = "Local Tour",
                    description = "Guided tour of nearby attractions"
                )
            )
        )
        scheduleDaysList.add(day1)
        
        // Day 2
        val day2 = ScheduleDay(
            dayNumber = 2,
            title = "Exploring ${trip.destination.split(",").firstOrNull() ?: trip.destination}",
            date = getNextDay(trip.startDate, 1),
            activities = listOf(
                ScheduleActivity(
                    time = "9:00 AM",
                    title = "Breakfast at Hotel",
                    description = "Continental breakfast included"
                ),
                ScheduleActivity(
                    time = "10:30 AM",
                    title = "Visit Main Attractions",
                    description = "City landmarks tour"
                ),
                ScheduleActivity(
                    time = "7:00 PM",
                    title = "Dinner at Local Restaurant",
                    description = "Experience local cuisine"
                )
            )
        )
        scheduleDaysList.add(day2)
        
        // Day 3
        val day3 = ScheduleDay(
            dayNumber = 3,
            title = "Departure",
            date = getNextDay(trip.startDate, 2),
            activities = listOf(
                ScheduleActivity(
                    time = "8:00 AM",
                    title = "Breakfast",
                    description = "Last meal at hotel"
                ),
                ScheduleActivity(
                    time = "10:00 AM",
                    title = "Check-out",
                    description = "Hotel ${trip.destination}"
                ),
                ScheduleActivity(
                    time = "1:00 PM",
                    title = "Departure from Airport",
                    description = "Flight back home"
                )
            )
        )
        scheduleDaysList.add(day3)
        
        _scheduleDays.value = scheduleDaysList
    }

    private fun getNextDay(dateString: String, daysToAdd: Int): String {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: return dateString
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
            
            return dateFormat.format(calendar.time)
        } catch (e: Exception) {
            return dateString
        }
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
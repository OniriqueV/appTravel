package com.datn.apptravel.ui.trip.model

import com.datn.apptravel.data.model.PlanType

data class ScheduleActivity(
    val id: String? = null,
    val tripId: String? = null,
    val time: String,
    val title: String,
    val description: String,
    val location: String? = null,
    val type: PlanType? = null,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val iconResId: Int? = null,
    val fullStartTime: String? = null,  // Full ISO datetime from Plan.startTime
    val endTime: String? = null,  // End time for activities
    val checkInDate: String? = null,  // For lodging
    val checkOutDate: String? = null,  // For lodging
    val arrivalDate: String? = null,  // For flight
    val arrivalTime: String? = null,  // For boat
    val reservationDate: String? = null,  // For restaurant
    val reservationTime: String? = null  // For restaurant
)
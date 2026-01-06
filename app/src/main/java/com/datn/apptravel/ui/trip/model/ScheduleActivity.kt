package com.datn.apptravels.ui.trip.model

import com.datn.apptravels.data.model.PlanType

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
    val fullStartTime: String? = null,
    val endTime: String? = null,
    val checkInDate: String? = null,
    val checkOutDate: String? = null,
    val arrivalDate: String? = null,
    val arrivalTime: String? = null,
    val reservationDate: String? = null,
    val reservationTime: String? = null
)
package com.datn.apptravels.data.model.request

data class CreateRestaurantPlanRequest(
    val tripId: String,
    val title: String,
    val address: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: String = "RESTAURANT",
    
    // Restaurant-specific fields
    val reservationDate: String? = null,
    val reservationTime: String? = null
)

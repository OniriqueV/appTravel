package com.datn.apptravels.data.model.request

data class CreateCarRentalPlanRequest(
    val tripId: String,
    val title: String,
    val address: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: String = "CAR_RENTAL",
    
    // CarRental-specific fields
    val pickupDate: String? = null,
    val pickupTime: String? = null,
    val phone: String? = null
)

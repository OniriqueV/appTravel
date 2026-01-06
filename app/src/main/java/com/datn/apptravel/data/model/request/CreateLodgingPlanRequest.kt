package com.datn.apptravels.data.model.request

data class CreateLodgingPlanRequest(
    val tripId: String,
    val title: String,
    val address: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: String = "LODGING",
    
    // Lodging-specific fields
    val checkInDate: String? = null,
    val checkOutDate: String? = null,
    val phone: String? = null
)

package com.datn.apptravels.data.model.request

data class CreateFlightPlanRequest(
    val tripId: String,
    val title: String,
    val address: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: String = "FLIGHT",
    
    // Flight-specific fields
    val arrivalLocation: String? = null,
    val arrivalAddress: String? = null,
    val arrivalDate: String? = null
)

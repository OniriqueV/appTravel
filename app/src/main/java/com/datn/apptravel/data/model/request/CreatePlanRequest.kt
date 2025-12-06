package com.datn.apptravel.data.model.request

data class CreatePlanRequest(
    val tripId: String? = null,
    val title: String? = null,
    val address: String? = null,
    val location: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val photos: List<String>? = null,
    val type: String? = null
)

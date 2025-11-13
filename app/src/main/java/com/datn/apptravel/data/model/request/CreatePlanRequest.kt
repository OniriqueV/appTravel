package com.datn.apptravel.data.model.request

import com.datn.apptravel.data.model.PlanType

data class CreatePlanRequest(
    val tripId: String,
    val title: String,
    val address: String? = null,
    val location: String? = null,
    val startTime: String,
    val endTime: String,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: String
)

package com.datn.apptravel.data.model.request

data class CreateTripRequest(
    val userId: String,
    val title: String,
    val startDate: String,
    val endDate: String,
    val isPublic: String = "none",
    val coverPhoto: String? = null,
    val content: String? = null,
    val tags: String? = null,
    val sharedAt: String? = null
)
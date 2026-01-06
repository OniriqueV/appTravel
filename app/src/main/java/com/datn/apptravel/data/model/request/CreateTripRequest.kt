package com.datn.apptravel.data.model.request

import com.datn.apptravel.data.model.User

data class CreateTripRequest(
    val userId: String,
    val title: String,
    val startDate: String?,
    val endDate: String?,
    val isPublic: String = "none",
    val coverPhoto: String? = null,
    val content: String? = null,
    val tags: String? = null,
    val members: List<User>? = null,
    val sharedWithUsers: List<User>? = null,
    val sharedAt: String? = null
)
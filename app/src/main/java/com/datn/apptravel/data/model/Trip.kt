package com.datn.apptravel.data.model


data class Trip(
    val id: Long,
    val userId: Long,
    val title: String,
    val startDate: String,          // Format: yyyy-MM-dd
    val endDate: String,            // Format: yyyy-MM-dd
    val isPublic: Boolean = false,
    val coverPhoto: String? = null,
    val plans: List<Plan>? = null,
    val createdAt: String? = null
)
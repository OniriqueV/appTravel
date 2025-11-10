package com.datn.apptravel.data.model


data class Plan(
    val id: Long,
    val tripId: Long,
    val title: String,
    val location: String? = null,
    val startTime: String,          //  format: yyyy-MM-dd'T'HH:mm:ss
    val endTime: String,            //  format: yyyy-MM-dd'T'HH:mm:ss
    val expense: Double? = null,
    val photoUrl: String? = null,
    val type: PlanType,
    val likesCount: Int = 0,        // Simplified - count instead of full list
    val commentsCount: Int = 0,     // Simplified - count instead of full list
    val createdAt: String? = null
)
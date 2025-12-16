package com.datn.apptravel.data.model

open class Plan(
    open val id: String? = null,
    open val tripId: String,
    open val title: String,
    open val address: String? = null,
    open val location: String? = null,
    open val startTime: String,          //  format: yyyy-MM-dd'T'HH:mm:ss
    open val expense: Double? = null,
    open val photoUrl: String? = null,
    open val photos: List<String>? = null,  // Collection of photos (filenames)
    open val type: PlanType,
    open val likesCount: Int = 0,        // Simplified - count instead of full list
    open val commentsCount: Int = 0,     // Simplified - count instead of full list
    open val createdAt: String? = null
)
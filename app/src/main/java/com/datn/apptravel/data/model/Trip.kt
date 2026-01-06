package com.datn.apptravel.data.model

data class Trip(
    val id: String? = null,
    val userId: String,
    val title: String,
    val startDate: String, // Format: yyyy-MM-dd
    val endDate: String, // Format: yyyy-MM-dd
    val isPublic: String = "none", // Values: "none", "public", "follower"
    val coverPhoto: String? = null,
    val content: String? = null,
    val tags: String? = null,
    val plans: List<Plan>? = null,
    val members: List<User>? = null, // List of members who can participate and manage plans in the trip
    val sharedWithUsers: List<User>? = null, // When isPublic="follower", share only with these users; empty = all followers
    val createdAt: String? = null,
    val sharedAt: String? = null,
    var hasConflict: Boolean = false // Client-side flag to indicate time conflict with another trip
)
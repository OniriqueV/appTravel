package com.datn.apptravels.ui.discover.model

data class ShareTripRequest(
    val tripId: String,
    val content: String,
    val tags: String,
    val isPublic: String, // PUBLIC | FOLLOWER
    val sharedWithUsers: List<String> = emptyList()
)
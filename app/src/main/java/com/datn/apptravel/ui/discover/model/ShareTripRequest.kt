package com.datn.apptravel.ui.discover.model

data class ShareTripRequest(
    val tripId: String,
    val content: String,
    val tags: String,
    val isPublic: String
)
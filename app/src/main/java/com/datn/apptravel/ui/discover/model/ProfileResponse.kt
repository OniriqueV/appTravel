package com.datn.apptravels.ui.discover.model

data class ProfileResponse(
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val followerCount: Long
)

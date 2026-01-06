package com.datn.apptravel.ui.discover.model

data class FollowRequest(
    val followerId: String,
    val followingId: String
)

data class FollowStatusResponse(
    val isFollowing: Boolean
)
package com.datn.apptravel.ui.discover.model

data class FollowUser(
    val id: String,
    val name: String,
    val avatarRes: Int,
    val bio: String? = null
)
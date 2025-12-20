package com.datn.apptravel.ui.discover.model


data class DiscoverItem(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,

    val tripId: String = "",
    val tripImage: String? = null,

    val caption: String? = null,
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val isLiked: Boolean = false,
    val isPublic: Boolean = true,
    val createdAt: Long = 0L
)

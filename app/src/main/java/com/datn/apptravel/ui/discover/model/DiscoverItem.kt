package com.datn.apptravel.ui.discover.model

data class DiscoverItem(
    val postId: String,
    val title: String?,
    val coverPhoto: String?,     // ảnh đại diện post
    val createdAt: Long,

    val tags: List<String>?,

    val userId: String?,
    val userName: String?,
    val userAvatar: String?,

    val likesCount: Int,
    val commentsCount: Int
)

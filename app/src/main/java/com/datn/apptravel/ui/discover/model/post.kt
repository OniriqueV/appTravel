package com.datn.apptravel.ui.discover.model

data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarRes: Int,
    val imageRes: Int,
    val caption: String,
    val location: String? = null,
    var likes: Int = 0,
    var isLiked: Boolean = false,
    var comments: List<String> = emptyList()
)

package com.datn.apptravel.ui.discover.model

data class CreatePostCommentRequest(
    val userId: String,
    val userName: String?,
    val avatar: String?,
    val content: String
)
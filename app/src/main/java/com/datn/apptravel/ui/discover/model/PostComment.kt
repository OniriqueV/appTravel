package com.datn.apptravel.ui.discover.model

data class PostComment(
    val commentId: String,
    val userId: String,
    val userName: String?,
    val avatar: String?,
    val content: String,
    val createdAt: Long
)
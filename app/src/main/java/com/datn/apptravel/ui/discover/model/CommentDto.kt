package com.datn.apptravels.ui.discover.model

data class CommentDto(
    val id: Long,
    val planId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val parentId: String?, // For replies
    val content: String,
    val createdAt: Long // Timestamp in milliseconds
)

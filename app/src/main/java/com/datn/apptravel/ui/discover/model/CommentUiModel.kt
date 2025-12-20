package com.datn.apptravel.ui.discover.model

data class CommentUiModel(
    val id: String,
    val userName: String,
    val userAvatar: String?,
    val content: String,
    val createdAt: Long,
    val isMine: Boolean = false
)
package com.datn.apptravels.ui.discover.model

data class PlanCommentDto(
    val id: Long,
    val userId: String,
    val userName: String?,
    val userAvatar: String?,
    val content: String,
    val createdAt: String?,
    val parentId: String? // 🔥 BẮT BUỘC
)

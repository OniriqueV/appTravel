package com.datn.apptravel.ui.discover.model
import com.google.gson.annotations.SerializedName

data class PostUiModel(
    val postId: String,
    val caption: String,
    val isPublic: Boolean,
    val createdAt: Long,

    val userId: String?,
    val userName: String?,
    val userAvatarUrl: String?,

    val tripId: String?,
    val tripImage: String?,

    val likeCount: Long,
    val commentCount: Long,
    @SerializedName("liked")
    val isLiked: Boolean
)

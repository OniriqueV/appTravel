package com.datn.apptravel.ui.discover.model

import com.google.gson.annotations.SerializedName

data class PostComment(
    val commentId: String? = null,
    val postId: String? = null,
    val userId: String? = null,
    val userName: String? = null,

    @SerializedName(value = "userAvatarUrl", alternate = ["avatarUrl", "avatar"])
    val userAvatarUrl: String? = null,

    val content: String? = null,
    val createdAt: Long? = null
)

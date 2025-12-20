package com.datn.apptravel.ui.discover.model

import com.google.gson.annotations.SerializedName

data class User(
    val userId: String? = null,

    @SerializedName(value = "userName", alternate = ["username", "name"])
    val userName: String? = null,

    @SerializedName(value = "avatarUrl", alternate = ["avatar", "userAvatarUrl", "photoUrl"])
    val avatarUrl: String? = null,

    // để sau này làm follow/unfollow vẫn dùng được
    val following: List<String> = emptyList()
)

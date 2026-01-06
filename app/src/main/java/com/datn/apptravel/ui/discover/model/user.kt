package com.datn.apptravels.ui.discover.model

import com.google.gson.annotations.SerializedName

data class User(

    // ID user (BE có thể trả id hoặc userId)
    @SerializedName(value = "userId", alternate = ["id"])
    val userId: String? = null,

    // Tên hiển thị
    // BE có thể là: userName | username | name | firstName | fullName
    @SerializedName(
        value = "userName",
        alternate = [
            "username",
            "name",
            "firstName",
            "fullName"
        ]
    )
    val userName: String? = null,

    // Avatar
    // BE có thể là: avatar | profilePicture | photoUrl | userAvatarUrl
    @SerializedName(
        value = "avatarUrl",
        alternate = [
            "avatar",
            "profilePicture",
            "photoUrl",
            "userAvatarUrl"
        ]
    )
    val avatarUrl: String? = null,

    // Dùng cho follow/unfollow (không ảnh hưởng màn này)
    val following: List<String> = emptyList()
)

package com.datn.apptravel.ui.discover.model

data class DiscoverItem(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,

    val tripId: String = "",
    val tripImage: String? = null,

    val caption: String? = null,
    val tags: String? = null,

    // 🔥 BE trả String ("public" | "follower")
    val isPublic: String = "public",

    // 🔥 BE trả ISO String
    val sharedAt: String = "",

    var isFollowing: Boolean = false, // ⭐ THÊM
)

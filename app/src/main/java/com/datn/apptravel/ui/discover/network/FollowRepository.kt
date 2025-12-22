package com.datn.apptravel.ui.discover.network

class FollowRepository(
    private val api: FollowApi
) {

    suspend fun follow(followerId: String, followingId: String) {
        api.follow(followerId, followingId)
    }

    suspend fun unfollow(followerId: String, followingId: String) {
        api.unfollow(followerId, followingId)
    }

    suspend fun isFollowing(
        followerId: String,
        followingId: String
    ): Boolean {
        return api.isFollowing(followerId, followingId).isFollowing
    }
}

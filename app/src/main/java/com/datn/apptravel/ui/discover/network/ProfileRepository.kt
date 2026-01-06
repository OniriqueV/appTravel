package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.User
import com.datn.apptravel.ui.discover.profileFollow.model.FollowerDto

class ProfileRepository(
    private val api: ProfileApi
) {
    suspend fun getUserTrips(
        userId: String,
        viewerId: String?
    ): List<DiscoverItem> {
        return api.getUserTrips(userId, viewerId)
    }
    suspend fun getProfile(userId: String) = api.getProfile(userId)
    suspend fun getFollowers(userId: String): List<FollowerDto> {
        return api.getFollowers(userId)
    }

}
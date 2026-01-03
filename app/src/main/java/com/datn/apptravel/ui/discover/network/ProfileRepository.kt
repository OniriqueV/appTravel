package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem

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

}
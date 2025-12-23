package com.datn.apptravel.ui.discover.profileFollow

import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.network.ProfileApi
import com.datn.apptravel.ui.discover.model.ProfileResponse


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

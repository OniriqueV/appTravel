package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.ShareTripRequest

class DiscoverRepository(
    private val api: DiscoverApi = DiscoverApiClient.api
) {

    // ================= FEED =================

    suspend fun getDiscover(
        userId: String?,
        page: Int,
        size: Int
    ): List<DiscoverItem> =
        api.getDiscover(userId, page, size)

    suspend fun getFollowing(
        userId: String,
        page: Int,
        size: Int
    ): List<DiscoverItem> =
        api.getFollowing(userId, page, size)


    suspend fun shareTrip(req: ShareTripRequest): Result<Unit> {
        return try {
            api.shareTrip(req)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


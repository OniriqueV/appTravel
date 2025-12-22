package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem

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

}

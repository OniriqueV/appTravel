package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem
import retrofit2.http.GET
import retrofit2.http.Query
import com.datn.apptravel.ui.discover.network.DiscoverApiClient.api


interface DiscoverApi {

    // ================= DISCOVER - PUBLIC =================
    @GET("api/discover")
    suspend fun getDiscover(
        @Query("userId") userId: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<DiscoverItem> =
        api.getDiscover(userId, page, size)

    // ================= DISCOVER - FOLLOWING =================
    @GET("api/discover/following")
    suspend fun getFollowing(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<DiscoverItem> =
        api.getDiscover(userId, page, size)
}

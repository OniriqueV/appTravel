package com.datn.apptravels.ui.discover.network

import com.datn.apptravels.ui.discover.model.DiscoverItem
import com.datn.apptravels.ui.discover.model.ShareTripRequest
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST


interface DiscoverApi {

    // ================= DISCOVER - PUBLIC =================
    @GET("api/discover")
    suspend fun getDiscover(
        @Query("userId") userId: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<DiscoverItem>

    // ================= DISCOVER - FOLLOWING =================
    @GET("api/discover/following")
    suspend fun getFollowing(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<DiscoverItem>

    // ================= SHARE TRIP =================
    @POST("api/discover/share-trip")
    suspend fun shareTrip(
        @Body req: ShareTripRequest
    )
}

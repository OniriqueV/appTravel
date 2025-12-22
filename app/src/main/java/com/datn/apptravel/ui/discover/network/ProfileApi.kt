package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    @GET("/api/discover/user/{userId}")
    suspend fun getUserTrips(
        @Path("userId") userId: String,
        @Query("viewerId") viewerId: String?
    ): List<DiscoverItem>
}

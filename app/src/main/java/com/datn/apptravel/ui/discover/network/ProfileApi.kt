package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.ProfileResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    @GET("/api/profile/{userId}/trips")
    suspend fun getUserTrips(
        @Path("userId") userId: String,
        @Query("viewerId") viewerId: String?
    ): List<DiscoverItem>

    @GET("/api/profile/{userId}")
    suspend fun getProfile(
        @Path("userId") userId: String
    ): ProfileResponse
}
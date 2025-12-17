package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.TripItem
import retrofit2.http.GET
import retrofit2.http.Query

interface TripApi {

    @GET("/api/trips/my")
    suspend fun getMyTrips(
        @Query("userId") userId: String
    ): List<TripItem>
}

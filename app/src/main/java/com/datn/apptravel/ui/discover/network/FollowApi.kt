package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.FollowStatusResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FollowApi {

    @POST("api/follow")
    suspend fun follow(
        @Query("followerId") followerId: String,
        @Query("followingId") followingId: String
    )

    @DELETE("api/follow")
    suspend fun unfollow(
        @Query("followerId") followerId: String,
        @Query("followingId") followingId: String
    )

    @GET("api/follow/status")
    suspend fun isFollowing(
        @Query("followerId") followerId: String,
        @Query("followingId") followingId: String
    ): Boolean
}

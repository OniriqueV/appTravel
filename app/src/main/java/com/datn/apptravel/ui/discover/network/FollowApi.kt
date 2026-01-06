package com.datn.apptravel.ui.discover.network


import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.datn.apptravel.ui.discover.model.User
import retrofit2.http.Path


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

    @GET("/api/follow/{userId}/followers/raw")
    suspend fun getFollowersRaw(
        @Path("userId") userId: String
    ): List<User>
}

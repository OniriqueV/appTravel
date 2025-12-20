package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.*
import retrofit2.http.*

interface DiscoverApi {

    // ================= DISCOVER =================
    @GET("api/discover")
    suspend fun getDiscover(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String
    ): List<DiscoverItem>

    @GET("api/discover/following")
    suspend fun getFollowing(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): List<DiscoverItem>

    // ================= POST DETAIL =================
    @GET("api/discover/posts/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: String,
        @Query("userId") userId: String? = null
    ): PostUiModel

    // ================= CREATE POST =================
    @POST("api/discover/posts")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): PostUiModel

    // ================= LIKE =================
    @POST("api/discover/posts/{postId}/like")
    suspend fun likePost(
        @Path("postId") postId: String,
        @Query("userId") userId: String
    )

    @DELETE("api/discover/posts/{postId}/like")
    suspend fun unlikePost(
        @Path("postId") postId: String,
        @Query("userId") userId: String
    )

    // ================= COMMENT =================
    @POST("api/discover/posts/{postId}/comments")
    suspend fun addPostComment(
        @Path("postId") postId: String,
        @Body request: CreatePostCommentRequest
    )

    @GET("api/discover/posts/{postId}/comments")
    suspend fun getPostComments(
        @Path("postId") postId: String
    ): List<PostComment>
}

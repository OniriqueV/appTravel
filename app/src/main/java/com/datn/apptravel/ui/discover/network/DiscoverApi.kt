package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.CreatePostCommentRequest
import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.PostComment
import com.datn.apptravel.ui.discover.model.PostDetailResponse
import retrofit2.http.*

interface DiscoverApi {

    @GET("discover")
    suspend fun getDiscover(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "newest"
    ): List<DiscoverItem>

    @GET("discover/following")
    suspend fun getFollowing(
        @Query("userId") userId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): List<DiscoverItem>

    @GET("discover/post/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: String,
        @Query("userId") userId: String?
    ): PostDetailResponse


    @POST("discover/post")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): Map<String, Any>      // backend trả { postId, message }

    @POST("discover/post/{postId}/like")
    suspend fun likePost(
        @Path("postId") postId: String,
        @Query("userId") userId: String
    )

    @DELETE("discover/post/{postId}/like")
    suspend fun unlikePost(
        @Path("postId") postId: String,
        @Query("userId") userId: String
    )

    @GET("/api/discover/post/{postId}/comment")
    suspend fun getPostComments(
        @Path("postId") postId: String
    ): List<PostComment>

    @POST("/api/discover/post/{postId}/comment")
    suspend fun addPostComment(
        @Path("postId") postId: String,
        @Body req: CreatePostCommentRequest
    )

    @GET("discover/search")
    suspend fun searchDiscover(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): List<DiscoverItem>


}

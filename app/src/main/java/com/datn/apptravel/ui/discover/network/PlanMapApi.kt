package com.datn.apptravels.ui.discover.network

import com.datn.apptravels.ui.discover.model.CommentRequest
import retrofit2.http.*
import com.datn.apptravels.ui.discover.model.PlanMapDetailResponse
import com.datn.apptravels.ui.discover.model.PlanCommentDto

interface PlanMapApi {

    @GET("/api/plans/{planId}")
    suspend fun getPlanDetail(
        @Path("planId") planId: String,
        @Header("X-USER-ID") userId: String
    ): PlanMapDetailResponse

    @POST("/api/plans/{planId}/like")
    suspend fun likePlan(
        @Path("planId") planId: String,
        @Header("X-USER-ID") userId: String
    ): PlanMapDetailResponse

    @DELETE("/api/plans/{planId}/like")
    suspend fun unlikePlan(
        @Path("planId") planId: String,
        @Header("X-USER-ID") userId: String
    ): PlanMapDetailResponse

    @GET("/api/plans/{planId}/comments")
    suspend fun getComments(
        @Path("planId") planId: String
    ): retrofit2.Response<List<PlanCommentDto>>


    @POST("/api/plans/{planId}/comments")
    suspend fun postComment(
        @Path("planId") planId: String,
        @Header("X-USER-ID") userId: String,
        @Body body: CommentRequest
    ): retrofit2.Response<Unit>


    @DELETE("/api/plans/{planId}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("planId") planId: String,
        @Path("commentId") commentId: Long,
        @Header("X-USER-ID") userId: String
    ): retrofit2.Response<Unit>


}

package com.datn.apptravel.ui.discover.network

import retrofit2.http.*
import com.datn.apptravel.ui.discover.model.PlanMapDetailResponse
import com.datn.apptravel.ui.discover.model.PlanCommentDto

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
    ): List<PlanCommentDto>

    @POST("/api/plans/{planId}/comments")
    suspend fun postComment(
        @Path("planId") planId: String,
        @Header("X-USER-ID") userId: String,
        @Body content: String
    )
}

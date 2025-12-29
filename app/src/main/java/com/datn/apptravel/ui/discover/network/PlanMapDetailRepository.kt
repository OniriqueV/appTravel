package com.datn.apptravel.ui.discover.network

class PlanMapDetailRepository(
    private val api: PlanMapApi
) {

    suspend fun getPlanDetail(planId: String, userId: String) =
        api.getPlanDetail(planId, userId)

    suspend fun likePlan(planId: String, userId: String) =
        api.likePlan(planId, userId)

    suspend fun unlikePlan(planId: String, userId: String) =
        api.unlikePlan(planId, userId)

    suspend fun getComments(planId: String) =
        api.getComments(planId)

    suspend fun postComment(planId: String, userId: String, content: String) =
        api.postComment(planId, userId, content)
}

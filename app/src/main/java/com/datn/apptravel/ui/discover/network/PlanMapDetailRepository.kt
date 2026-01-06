package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.CommentRequest
import com.datn.apptravel.ui.discover.model.PlanCommentDto

class PlanMapDetailRepository(
    private val api: PlanMapApi
) {

    suspend fun getPlanDetail(planId: String, userId: String) =
        api.getPlanDetail(planId, userId)

    suspend fun likePlan(planId: String, userId: String) =
        api.likePlan(planId, userId)

    suspend fun unlikePlan(planId: String, userId: String) =
        api.unlikePlan(planId, userId)

    suspend fun getComments(planId: String): List<PlanCommentDto> {
        return try {
            val res = api.getComments(planId)
            if (res.isSuccessful) {
                res.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun deleteComment(
        planId: String,
        commentId: Long,
        userId: String
    ) {
        api.deleteComment(planId, commentId, userId)
    }

    suspend fun postComment(
        planId: String,
        userId: String,
        content: String,
        parentId: String?
    ) {
        api.postComment(
            planId,
            userId,
            CommentRequest(
                content = content,
                parentId = parentId
            )
        )
    }

}

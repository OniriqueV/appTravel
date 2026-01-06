package com.datn.apptravel.ui.discover.PlanMap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.ui.discover.model.PlanCommentDto
import com.datn.apptravel.ui.discover.model.PlanMapDetailResponse
import com.datn.apptravel.ui.discover.network.PlanMapDetailRepository
import kotlinx.coroutines.launch

class PlanMapDetailViewModel(
    private val repo: PlanMapDetailRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _plan = MutableLiveData<PlanMapDetailResponse>()
    val plan: LiveData<PlanMapDetailResponse> = _plan

    private val _comments = MutableLiveData<List<PlanCommentDto>>()
    val comments: LiveData<List<PlanCommentDto>> = _comments

    // 🔥 giữ planId nội bộ để khỏi truyền qua lại
    private var currentPlanId: String? = null
    private var isLikeLoading = false

    /* =========================
       LOAD PLAN DETAIL
       ========================= */
    fun loadPlan(planId: String) = viewModelScope.launch {
        val userId = session.getUserId() ?: return@launch

        currentPlanId = planId

        try {
            _plan.value = repo.getPlanDetail(planId, userId)
            _comments.value = repo.getComments(planId)
        } catch (e: Exception) {
            // TODO: handle error (log / show toast)
        }
    }

    /* =========================
       LIKE / UNLIKE
       ========================= */
    fun toggleLike() = viewModelScope.launch {
        if (isLikeLoading) return@launch

        val planId = currentPlanId ?: return@launch
        val userId = session.getUserId() ?: return@launch
        val current = _plan.value ?: return@launch

        isLikeLoading = true

        try {
            _plan.value = if (current.liked) {
                repo.unlikePlan(planId, userId)
            } else {
                repo.likePlan(planId, userId)
            }
        } catch (e: Exception) {
            // TODO: handle error (toast/log)
        } finally {
            isLikeLoading = false
        }
    }


    /* =========================
       POST COMMENT
       ========================= */
    fun postComment(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch

        val planId = currentPlanId ?: return@launch
        val userId = session.getUserId() ?: return@launch

        try {
            repo.postComment(planId, userId, text)

            // reload comments
            _comments.value = repo.getComments(planId)

            // update comment count
            _plan.value = _plan.value?.copy(
                commentCount = (_plan.value?.commentCount ?: 0) + 1
            )
        } catch (e: Exception) {
            // TODO: handle error
        }
    }
}

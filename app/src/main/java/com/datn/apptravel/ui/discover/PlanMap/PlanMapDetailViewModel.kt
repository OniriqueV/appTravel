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

    private val _storyImages = MutableLiveData<List<String>>()
    val storyImages: LiveData<List<String>> = _storyImages

    private val _plan = MutableLiveData<PlanMapDetailResponse>()
    val plan: LiveData<PlanMapDetailResponse> = _plan

    private val _isOwner = MutableLiveData<Boolean>()
    val isOwnerLive: LiveData<Boolean> = _isOwner

    private val _comments = MutableLiveData<List<PlanCommentDto>>()
    val comments: LiveData<List<PlanCommentDto>> = _comments

    private var currentPlanId: String? = null
    private var isLikeLoading = false

    /* =========================
       LOAD PLAN DETAIL
       ========================= */
    fun loadPlan(planId: String) = viewModelScope.launch {
        val userId = session.getUserId() ?: return@launch
        currentPlanId = planId

        try {
            val response = repo.getPlanDetail(planId, userId)

            _plan.value = response
            _storyImages.value = response.images

            // 🔥 CHỈ SỬA 1 DÒNG Ở ĐÂY
            _comments.value = sortComments(repo.getComments(planId))

            _isOwner.value = response.isOwner

        } catch (e: Exception) {
            // TODO handle error
        }
    }

    /* =========================
       LIKE / UNLIKE (GIỮ NGUYÊN)
       ========================= */
    fun toggleLike() = viewModelScope.launch {
        if (isLikeLoading) return@launch

        val planId = currentPlanId ?: return@launch
        val userId = session.getUserId() ?: return@launch
        val current = _plan.value ?: return@launch

        _plan.value = current.copy(
            liked = !current.liked,
            likeCount = if (current.liked)
                current.likeCount - 1
            else
                current.likeCount + 1
        )

        isLikeLoading = true
        try {
            if (current.liked) {
                repo.unlikePlan(planId, userId)
            } else {
                repo.likePlan(planId, userId)
            }
        } catch (e: Exception) {
            _plan.value = current
        } finally {
            isLikeLoading = false
        }
    }

    /* =========================
       POST COMMENT
       ========================= */

    fun loadComments() = viewModelScope.launch {
        val planId = currentPlanId ?: return@launch
        _comments.value = sortComments(repo.getComments(planId))
    }

    fun deleteComment(commentId: Long) = viewModelScope.launch {
        val planId = currentPlanId ?: return@launch
        val userId = session.getUserId() ?: return@launch

        try {
            repo.deleteComment(planId, commentId, userId)

            // 🔥 CHỈ SỬA 1 DÒNG
            _comments.value = sortComments(repo.getComments(planId))

            _plan.value = _plan.value?.copy(
                commentCount = (_plan.value?.commentCount ?: 1) - 1
            )
        } catch (e: Exception) {
            // TODO handle error
        }
    }

    fun isOwner(): Boolean = plan.value?.isOwner == true
    fun currentUserId(): String? = session.getUserId()

    /* =========================
       🔥 CORE COMMENT LOGIC
       KHÔNG ẢNH HƯỞNG PHẦN KHÁC
       ========================= */
    private fun sortComments(
        list: List<PlanCommentDto>
    ): List<PlanCommentDto> {

        val result = mutableListOf<PlanCommentDto>()

        val parents = list.filter { it.parentId == null }
        val replies = list.filter { it.parentId != null }
            .groupBy { it.parentId }

        parents.forEach { parent ->
            result.add(parent)
            replies[parent.id.toString()]?.let {
                result.addAll(it)
            }
        }

        return result
    }

    fun postComment(text: String, parentId: String? = null ) = viewModelScope.launch {
        if (text.isBlank()) return@launch

        val planId = currentPlanId ?: return@launch
        val userId = session.getUserId() ?: return@launch

        try {
            repo.postComment(planId, userId, text, parentId)
            _comments.value = sortComments(repo.getComments(planId))
        } catch (e: Exception) { }
    }


}

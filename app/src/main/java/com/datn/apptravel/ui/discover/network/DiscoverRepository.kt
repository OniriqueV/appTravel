package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.*

class DiscoverRepository(
    private val api: DiscoverApi = DiscoverApiClient.api
) {

    // ================= FEED =================

    suspend fun getDiscover(
        page: Int,
        size: Int,
        sort: String
    ): List<DiscoverItem> =
        api.getDiscover(page, size, sort)

    suspend fun getFollowing(
        userId: String,
        page: Int,
        size: Int
    ): List<DiscoverItem> =
        api.getFollowing(userId, page, size)

    // ================= POST DETAIL =================

    suspend fun getPostDetail(
        postId: String,
        userId: String?
    ): PostUiModel =
        api.getPostDetail(postId, userId)

    // ================= CREATE POST =================

    suspend fun createPost(request: CreatePostRequest) =
        api.createPost(request)

    // ================= LIKE / UNLIKE =================
    // ❗ KHÔNG toggle ở đây – để ViewModel quyết định

    suspend fun likePost(
        postId: String,
        userId: String
    ) =
        api.likePost(postId, userId)

    suspend fun unlikePost(
        postId: String,
        userId: String
    ) =
        api.unlikePost(postId, userId)

    // ================= COMMENT =================

    suspend fun addPostComment(
        postId: String,
        request: CreatePostCommentRequest
    ) =
        api.addPostComment(postId, request)

    suspend fun getPostComments(
        postId: String
    ): List<PostComment> =
        api.getPostComments(postId)
}

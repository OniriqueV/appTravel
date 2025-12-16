package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.PostDetailResponse

class DiscoverRepository(private val api: DiscoverApi) {

    /** Lấy danh sách discover chung */
    suspend fun getDiscover(page: Int = 0, size: Int = 10, sort: String = "newest"): List<DiscoverItem> {
        return api.getDiscover(page, size, sort)
    }

    /** Lấy danh sách bài của người dùng mà mình follow */
    suspend fun getFollowing(userId: String, page: Int = 0, size: Int = 10): List<DiscoverItem> {
        return api.getFollowing(userId, page, size)
    }

    /** Chi tiết bài viết */
    suspend fun getPostDetail(postId: String, userId: String?): PostDetailResponse {
        return api.getPostDetail(postId, userId)
    }

    /** Tạo bài viết mới */
    suspend fun createPost(request: CreatePostRequest): String {
        val resp = api.createPost(request)        // backend trả Map<String, Any>
        val postId = resp["postId"]?.toString() ?: ""
        return postId
    }

    suspend fun searchDiscover(query: String, page: Int = 0, size: Int = 10): List<DiscoverItem> {
        return api.searchDiscover(query, page, size)
    }


}

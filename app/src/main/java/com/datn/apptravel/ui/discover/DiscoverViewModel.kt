package com.datn.apptravel.ui.discover

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repository: DiscoverRepository
) : ViewModel() {

    // ================= FEED =================
    val discoverList = MutableLiveData<List<DiscoverItem>>()
    val followingList = MutableLiveData<List<DiscoverItem>>()
    val errorMessage = MutableLiveData<String?>()

    // ================= CREATE POST =================
    val isPosting = MutableLiveData(false)
    val postCreated = MutableLiveData(false)

    // -------------------------------------------------------
    // LOAD DISCOVER FEED
    // -------------------------------------------------------
    fun loadDiscover(
        page: Int = 0,
        size: Int = 10,
        sort: String = "newest"
    ) {
        viewModelScope.launch {
            try {
                discoverList.postValue(
                    repository.getDiscover(page, size, sort)
                )
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách khám phá")
            }
        }
    }

    // -------------------------------------------------------
    // LOAD FOLLOWING FEED
    // -------------------------------------------------------
    fun loadFollowing(
        userId: String,
        page: Int = 0,
        size: Int = 10
    ) {
        viewModelScope.launch {
            try {
                followingList.postValue(
                    repository.getFollowing(userId, page, size)
                )
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách Following")
            }
        }
    }

    // -------------------------------------------------------
    // CREATE POST
    // -------------------------------------------------------
    fun createPost(request: CreatePostRequest) {
        viewModelScope.launch {
            isPosting.postValue(true)
            try {
                repository.createPost(request)
                postCreated.postValue(true)
            } catch (e: Exception) {
                errorMessage.postValue("Đăng bài thất bại")
            } finally {
                isPosting.postValue(false)
            }
        }
    }

    // -------------------------------------------------------
    // LIKE FROM FEED (fire & forget)
    // -------------------------------------------------------
    fun likeFromFeed(postId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Feed không cần toggle logic
                // BE quyết định liked/unliked
                repository.likePost(postId, userId)
            } catch (e: Exception) {
                errorMessage.postValue("Không thể like bài viết")
            }
        }
    }

    fun clearPostCreated() {
        postCreated.value = false
    }

}

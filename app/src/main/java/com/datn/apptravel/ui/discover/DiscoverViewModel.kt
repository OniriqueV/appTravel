package com.datn.apptravel.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.model.PostDetailResponse
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repository: DiscoverRepository
) : ViewModel() {


    // Feed
    val discoverList = MutableLiveData<List<DiscoverItem>>()
    val followingList = MutableLiveData<List<DiscoverItem>>()

    // Post detail
    private val _postDetail = MutableLiveData<PostDetailResponse?>()
    val postDetail: LiveData<PostDetailResponse?> = _postDetail


    // Create Post
    val isPosting = MutableLiveData<Boolean>()
    val postCreated = MutableLiveData<String?>()   // trả về postId
    val errorMessage = MutableLiveData<String?>()

    // -------------------------------------------------------
    // LOAD DISCOVER FEED
    // -------------------------------------------------------
    fun loadDiscover(page: Int = 0, size: Int = 10, sort: String = "newest") {
        viewModelScope.launch {
            try {
                val result = repository.getDiscover(page, size, sort)
                discoverList.postValue(result)
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách khám phá")
                e.printStackTrace()
            }
        }
    }

    // -------------------------------------------------------
    // LOAD FOLLOWING FEED
    // -------------------------------------------------------
    fun loadFollowing(userId: String, page: Int = 0, size: Int = 10) {
        viewModelScope.launch {
            try {
                val result = repository.getFollowing(userId, page, size)
                followingList.postValue(result)
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách Following")
                e.printStackTrace()
            }
        }
    }

    // -------------------------------------------------------
    // GET POST DETAIL
    // -------------------------------------------------------
    fun getPostDetail(postId: String, userId: String?) {
        viewModelScope.launch {
            try {
                val result = repository.getPostDetail(postId, userId)
                _postDetail.postValue(result)
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được chi tiết bài viết")
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
                val postId = repository.createPost(request)
                postCreated.postValue(postId)    // update thành công
            } catch (e: Exception) {
                errorMessage.postValue("Đăng bài thất bại")
                e.printStackTrace()
            } finally {
                isPosting.postValue(false)
            }
        }
    }

}

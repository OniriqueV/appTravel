package com.datn.apptravel.ui.discover.detail

import androidx.lifecycle.*
import com.datn.apptravel.ui.discover.model.PostUiModel
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val repository: DiscoverRepository
) : ViewModel() {

    private val _postDetail = MutableLiveData<PostUiModel>()
    val postDetail: LiveData<PostUiModel> = _postDetail

    val errorMessage = MutableLiveData<String?>()

    fun loadPostDetail(postId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        viewModelScope.launch {
            try {
                val post = repository.getPostDetail(postId, userId)
                _postDetail.postValue(post)
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được bài viết")
            }
        }
    }

    fun toggleLike() {
        val post = _postDetail.value ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                if (post.isLiked) {
                    repository.unlikePost(post.postId, userId)
                } else {
                    repository.likePost(post.postId, userId)
                }

                // 🔥 reload từ BE → state luôn đúng
                loadPostDetail(post.postId)

            } catch (e: Exception) {
                errorMessage.postValue("Không thể cập nhật like")
            }
        }
    }
}

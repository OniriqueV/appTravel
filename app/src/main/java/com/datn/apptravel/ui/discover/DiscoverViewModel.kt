package com.datn.apptravel.ui.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.Post
import com.datn.apptravel.ui.discover.model.User

class DiscoverViewModel : ViewModel() {

    private val _randomPosts = MutableLiveData<List<Post>>()
    val randomPosts: LiveData<List<Post>> get() = _randomPosts

    private val _followingPosts = MutableLiveData<List<Post>>()
    val followingPosts: LiveData<List<Post>> get() = _followingPosts

    private val currentUser = User(
        userId = "current_user",
        username = "Me",
        avatar = R.drawable.ic_avatar_placeholder,
        following = listOf("u1", "u3")
    )

    private lateinit var allPosts: List<Post>

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        allPosts = listOf(
            Post(
                id = "p1",
                userId = "u1",
                userName = "Minh Travel",
                userAvatarRes = R.drawable.ic_avatar_placeholder,
                imageRes = R.drawable.img_sample_1,
                caption = "Khám phá đỉnh Phú Sĩ!",
                location = "Japan",
                likes = 10,
                comments = listOf("Đẹp quá!", "Muốn đi quá 🗻")
            ),
            Post(
                id = "p2",
                userId = "u2",
                userName = "Linh Journey",
                userAvatarRes = R.drawable.ic_avatar_placeholder,
                imageRes = R.drawable.img_sample_2,
                caption = "Hoàng hôn Đà Lạt 🌄",
                location = "Đà Lạt",
                likes = 8,
                comments = listOf("Xịn xò", "Nice shot!")
            ),
            Post(
                id = "p3",
                userId = "u3",
                userName = "Trip Mate",
                userAvatarRes = R.drawable.ic_avatar_placeholder,
                imageRes = R.drawable.img_sample_1,
                caption = "Camping cuối tuần cùng bạn bè!",
                location = "Đà Nẵng",
                likes = 15,
                comments = listOf("Tuyệt!", "Ảnh xịn!")
            )
        )

        _randomPosts.value = allPosts.shuffled()
        _followingPosts.value = allPosts.filter { post ->
            currentUser.following.contains(post.userId)
        }
    }

    fun toggleLike(post: Post) {
        post.isLiked = !post.isLiked
        if (post.isLiked) post.likes++ else post.likes--

        // cập nhật lại list để LiveData notify
        _randomPosts.value = _randomPosts.value?.toList()
        _followingPosts.value = _followingPosts.value?.toList()
    }
}

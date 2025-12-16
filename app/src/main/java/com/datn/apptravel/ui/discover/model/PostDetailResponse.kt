package com.datn.apptravel.ui.discover.model

data class PostDetailResponse(
    val post: PostDto,
    val user: UserDto,
    val trip: TripShortDto?,          // trip có thể null
    val likes: LikesInfoDto,
    val comments: List<CommentDto>
) {
    data class PostDto(
        val postId: String,
        val title: String,
        val content: String,
        val images: List<String>,
        val tags: List<String>,
        val createdAt: Long
    )

    data class UserDto(
        val userId: String,
        val userName: String,
        val avatar: String
    )

    data class TripShortDto(
        val tripId: String,
        val title: String,
        val coverPhoto: String
    )

    data class LikesInfoDto(
        val count: Int,
        val userLiked: Boolean
    )

    data class CommentDto(
        val commentId: String,
        val userId: String,
        val userName: String,
        val avatar: String,
        val content: String,
        val createdAt: Long
    )
}

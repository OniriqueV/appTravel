package com.datn.apptravel.ui.discover.model

data class CreatePostRequest(
    val userId: String,
    val tripId: String,
    val content: String? = null,
    val isPublic: Boolean = true,
    val tags: List<String> = emptyList()
)

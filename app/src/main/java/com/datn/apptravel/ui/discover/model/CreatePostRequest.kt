package com.datn.apptravel.ui.discover.model

data class CreatePostRequest(
    val userId: String,
    val title: String,
    val content: String,
    val images: List<String>,
    val tags: List<String>?,
    val tripId: String?
)

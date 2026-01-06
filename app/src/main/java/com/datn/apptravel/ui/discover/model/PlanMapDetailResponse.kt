package com.datn.apptravel.ui.discover.model

data class PlanMapDetailResponse(
    val planId: String,
    val title: String,
    val address: String?,
    val location: String?,
    val startTime: String?,
    val expense: Double?,
    val images: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    val liked: Boolean
)
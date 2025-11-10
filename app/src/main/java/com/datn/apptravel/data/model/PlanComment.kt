package com.datn.apptravel.data.model

data class PlanComment(
    val id: Long,
    val planId: Long,
    val userId: Long,
    val content: String,
    val createdAt: String
)
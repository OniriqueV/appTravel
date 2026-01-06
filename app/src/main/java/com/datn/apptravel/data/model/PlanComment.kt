package com.datn.apptravels.data.model

data class PlanComment(
    val id: Long,
    val planId: Long,
    val userId: Long,
    val content: String,
    val parentId: Long? = null,  // For nested/threaded comments (nullable for top-level comments)
    val createdAt: String
)
package com.datn.apptravels.ui.discover.model

data class CommentRequest(
    val content: String,
    val parentId: String? = null
)

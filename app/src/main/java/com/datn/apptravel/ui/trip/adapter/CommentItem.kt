package com.datn.apptravel.ui.trip.adapter

import com.datn.apptravel.ui.discover.model.CommentDto

data class CommentItem(
    val comment: CommentDto,
    val isReply: Boolean = false,
    val level: Int = 0,
    val replies: List<CommentDto> = emptyList(),
    var isRepliesVisible: Boolean = false
)

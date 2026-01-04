package com.datn.apptravel.ui.trip.adapter

import com.datn.apptravel.ui.discover.model.CommentDto

/**
 * Wrapper for displaying comments with hierarchy
 * Used to control visibility of replies
 */
data class CommentItem(
    val comment: CommentDto,
    val isReply: Boolean = false,
    val level: Int = 0,
    val replies: List<CommentDto> = emptyList(),
    var isRepliesVisible: Boolean = false
)

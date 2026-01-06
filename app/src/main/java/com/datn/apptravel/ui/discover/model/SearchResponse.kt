package com.datn.apptravels.ui.search.model

data class SearchResponse(
    val keyword: String,
    val page: Int,
    val size: Int,
    val trips: List<TripSearchDto>,
    val users: List<UserSearchDto>
)

data class TripSearchDto(
    val id: String,
    val userId: String,
    val title: String,
    val coverPhoto: String?,
    val content: String?,
    val tags: String?
)

data class UserSearchDto(
    val id: String,
    val fullName: String,
    val profilePicture: String?
)

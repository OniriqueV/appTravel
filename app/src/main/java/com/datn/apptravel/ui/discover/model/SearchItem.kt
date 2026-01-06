package com.datn.apptravels.ui.search

sealed class SearchItem {

    data class Header(
        val title: String
    ) : SearchItem()

    data class UserItem(
        val userId: String,
        val name: String,
        val avatar: String?
    ) : SearchItem()

    data class TripItem(
        val tripId: String,
        val title: String,
        val image: String?,
        val tags: String?
    ) : SearchItem()
}

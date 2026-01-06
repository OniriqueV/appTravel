package com.datn.apptravels.ui.search

import com.datn.apptravels.ui.search.network.SearchApi

class SearchRepository(
    private val api: SearchApi
) {
    suspend fun search(keyword: String) = api.search(keyword)
}

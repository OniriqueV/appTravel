package com.datn.apptravel.ui.search

import com.datn.apptravel.ui.search.network.SearchApi

class SearchRepository(
    private val api: SearchApi
) {
    suspend fun search(keyword: String) = api.search(keyword)
}

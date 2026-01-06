package com.datn.apptravels.ui.search.network

import com.datn.apptravels.ui.search.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("/api/search")
    suspend fun search(
        @Query("keyword") keyword: String
    ): SearchResponse
}

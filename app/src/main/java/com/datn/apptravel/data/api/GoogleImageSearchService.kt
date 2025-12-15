package com.datn.apptravel.data.api

import com.datn.apptravel.data.model.response.GoogleImageSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for Google Custom Search API (Image Search)
 */
interface GoogleImageSearchService {
    
    @GET("customsearch/v1")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("searchType") searchType: String = "image",
        @Query("num") num: Int = 2,
        @Query("imgSize") imgSize: String = "large"
    ): GoogleImageSearchResponse
}

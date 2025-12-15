package com.datn.apptravel.data.repository

import com.datn.apptravel.BuildConfig
import com.datn.apptravel.data.api.GoogleImageSearchService

class ImageSearchRepository(
    private val googleImageSearchService: GoogleImageSearchService
) {

    suspend fun searchImages(query: String, count: Int = 2): List<String> {
        return try {
            val response = googleImageSearchService.searchImages(
                apiKey = BuildConfig.GOOGLE_CUSTOM_SEARCH_API_KEY,
                cx = BuildConfig.GOOGLE_CUSTOM_SEARCH_CX,
                query = query,
                num = count
            )
            
            // Extract image links from response
            response.items?.mapNotNull { it.link } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("ImageSearchRepository", "Error searching images: ${e.message}", e)
            emptyList()
        }
    }
}

package com.datn.apptravel.data.repository

import com.datn.apptravel.BuildConfig
import com.datn.apptravel.data.api.GoogleImageSearchService

class ImageSearchRepository(
    private val googleImageSearchService: GoogleImageSearchService
) {

    suspend fun searchImages(query: String, count: Int = 2): List<String> {
        return try {
            android.util.Log.d("ImageSearchRepository", "Searching images for: $query")
            val response = googleImageSearchService.searchImages(
                apiKey = BuildConfig.GOOGLE_CUSTOM_SEARCH_API_KEY,
                cx = BuildConfig.GOOGLE_CUSTOM_SEARCH_CX,
                query = query,
                num = count
            )
            
            // Extract image links from response
            val imageUrls = response.items?.mapNotNull { it.link } ?: emptyList()
            android.util.Log.d("ImageSearchRepository", "Found ${imageUrls.size} images for query: $query")
            if (imageUrls.isNotEmpty()) {
                android.util.Log.d("ImageSearchRepository", "First image URL: ${imageUrls[0]}")
            }
            imageUrls
        } catch (e: Exception) {
            android.util.Log.e("ImageSearchRepository", "Error searching images for '$query': ${e.message}", e)
            emptyList()
        }
    }
}

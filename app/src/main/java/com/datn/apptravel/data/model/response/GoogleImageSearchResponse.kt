package com.datn.apptravels.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Google Custom Search API response model for image search
 */
data class GoogleImageSearchResponse(
    @SerializedName("items")
    val items: List<SearchItem>? = null
)

data class SearchItem(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("link")
    val link: String,
    
    @SerializedName("image")
    val image: ImageData? = null
)

data class ImageData(
    @SerializedName("thumbnailLink")
    val thumbnailLink: String? = null,
    
    @SerializedName("contextLink")
    val contextLink: String? = null
)

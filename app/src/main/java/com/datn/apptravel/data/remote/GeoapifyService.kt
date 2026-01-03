package com.datn.apptravel.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Geoapify Places API - Tìm địa điểm gần existing plans
 */
interface GeoapifyService {

    /**
     * Search places by category near a location
     * Categories: accommodation, restaurant, tourism, entertainment, etc.
     */
    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String, // "circle:lng,lat,radius_meters"
        @Query("bias") bias: String, // "proximity:lng,lat"
        @Query("limit") limit: Int = 20,
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyPlacesResponse>

    /**
     * Get place details by coordinates
     */
    @GET("v1/geocode/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyGeocodeResponse>
}

// Response models for Geoapify API
data class GeoapifyPlacesResponse(
    val features: List<GeoapifyFeature>
)

data class GeoapifyFeature(
    val properties: GeoapifyProperties,
    val geometry: GeoapifyGeometry
)

data class GeoapifyProperties(
    val name: String?,
    val address_line1: String?,
    val address_line2: String?,
    val categories: List<String>?,
    val distance: Double?
)

data class GeoapifyGeometry(
    val coordinates: List<Double> // [lng, lat]
)

data class GeoapifyGeocodeResponse(
    val features: List<GeoapifyFeature>
)

/**
 * Google Custom Search API - Lấy ảnh địa điểm
 */
interface GoogleImageService {

    @GET("customsearch/v1")
    suspend fun searchImages(
        @Query("q") query: String,
        @Query("searchType") searchType: String = "image",
        @Query("num") num: Int = 1,
        @Query("key") apiKey: String,
        @Query("cx") searchEngineId: String
    ): Response<GoogleImageResponse>
}

// Response models for Google Image API
data class GoogleImageResponse(
    val items: List<GoogleImageItem>?
)

data class GoogleImageItem(
    val link: String,
    val image: GoogleImageMetadata?
)

data class GoogleImageMetadata(
    val thumbnailLink: String?
)
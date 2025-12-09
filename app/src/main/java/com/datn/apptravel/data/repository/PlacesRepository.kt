package com.datn.apptravel.data.repository

import com.datn.apptravel.BuildConfig
import com.datn.apptravel.data.api.ApiService
import com.datn.apptravel.data.api.NetworkResult
import com.datn.apptravel.data.model.response.MapPlace

class PlacesRepository(private val apiService: ApiService) {
    
    companion object {
        private val API_KEY = BuildConfig.GEOAPIFY_API_KEY
        private const val DEFAULT_RADIUS = 30000
    }

    private fun getSampleImageUrl(category: String, placeName: String): String {
        // Using placeholder image service with category-specific images
        val categoryKey = when {
            category.contains("accommodation") || category.contains("lodging") -> "hotel"
            category.contains("restaurant") || category.contains("catering") -> "food"
            category.contains("tourism") || category.contains("attraction") -> "architecture"
            category.contains("airport") || category.contains("flight") -> "airport"
            category.contains("railway") || category.contains("train") -> "train"
            category.contains("boat") || category.contains("rental") -> "boat"
            category.contains("shopping") || category.contains("mall") -> "shopping"
            category.contains("religion") -> "temple"
            category.contains("theatre") || category.contains("entertainment") -> "theatre"
            else -> "city"
        }
        
        // Using Unsplash Source for placeholder images (you can also use Lorem Picsum)
        return "https://source.unsplash.com/800x600/?$categoryKey,travel"
    }

    private fun getSampleDescription(placeName: String, address: String?): String {
        return "$placeName is a wonderful place to visit. Located at ${address ?: "a great location"}, " +
                "it offers unique experiences and memorable moments for all visitors."
    }

    private fun getSampleGalleryImages(category: String): List<String> {
        val categoryKey = when {
            category.contains("accommodation") || category.contains("lodging") -> "hotel"
            category.contains("restaurant") || category.contains("catering") -> "food"
            category.contains("tourism") || category.contains("attraction") -> "architecture"
            else -> "travel"
        }
        
        return listOf(
            "https://source.unsplash.com/400x300/?$categoryKey,interior",
            "https://source.unsplash.com/400x300/?$categoryKey,view"
        )
    }

    suspend fun getPlacesByCategory(
        category: String,
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_RADIUS,
        limit: Int = 20
    ): NetworkResult<List<MapPlace>> {
        return try {
            val filter = "circle:$longitude,$latitude,$radius"
            android.util.Log.d("PlacesRepository", "getPlacesByCategory - Category: $category, Lat: $latitude, Lon: $longitude, Filter: $filter")
            
            val response = apiService.getPlaces(
                categories = category,
                filter = filter,
                limit = limit,
                apiKey = API_KEY
            )
            
            android.util.Log.d("PlacesRepository", "API Response - Success: ${response.isSuccessful}, Code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val places = response.body()!!.features?.mapNotNull { feature ->
                    val properties = feature.properties
                    if (properties?.lat != null && properties.lon != null) {
                        val placeName = properties.name ?: "Unknown"
                        MapPlace(
                            id = properties.placeId ?: "",
                            name = placeName,
                            latitude = properties.lat,
                            longitude = properties.lon,
                            address = properties.formatted ?: properties.addressLine1,
                            category = category,
                            imageUrl = getSampleImageUrl(category, placeName),
                            description = getSampleDescription(placeName, properties.formatted ?: properties.addressLine1),
                            galleryImages = getSampleGalleryImages(category)
                        )
                    } else null
                } ?: emptyList()
                
                NetworkResult.Success(places)
            } else {
                NetworkResult.Error("Failed to fetch places: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun searchPlaces(
        query: String,
        latitude: Double,
        longitude: Double,
        radius: Int = DEFAULT_RADIUS,
        limit: Int = 20
    ): NetworkResult<List<MapPlace>> {
        return try {
            android.util.Log.d("PlacesRepository", "searchPlaces - Query: $query")
            
            // Use Geocoding API to find the location's coordinates
            val response = apiService.geocodeSearch(
                text = query,
                apiKey = API_KEY,
                limit = 1
            )
            
            android.util.Log.d("PlacesRepository", "Geocoding Response - Success: ${response.isSuccessful}, Code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val places = response.body()!!.features?.mapNotNull { feature ->
                    val properties = feature.properties
                    if (properties?.lat != null && properties.lon != null) {
                        android.util.Log.d("PlacesRepository", "Geocoding found: ${properties.name ?: properties.formatted} at (${properties.lat}, ${properties.lon})")
                        
                        val placeName = properties.name ?: properties.formatted ?: query
                        MapPlace(
                            id = properties.placeId ?: "",
                            name = placeName,
                            latitude = properties.lat,
                            longitude = properties.lon,
                            address = properties.formatted ?: properties.addressLine1,
                            category = "location",
                            imageUrl = getSampleImageUrl("location", placeName),
                            description = getSampleDescription(placeName, properties.formatted ?: properties.addressLine1),
                            galleryImages = getSampleGalleryImages("location")
                        )
                    } else null
                } ?: emptyList()
                
                NetworkResult.Success(places)
            } else {
                NetworkResult.Error("Location not found: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Network error: ${e.message}")
        }
    }
}

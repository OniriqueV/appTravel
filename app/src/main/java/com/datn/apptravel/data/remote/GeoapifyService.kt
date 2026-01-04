//package com.datn.apptravel.data.remote
//
//import retrofit2.Response
//import retrofit2.http.GET
//import retrofit2.http.Query
//
///**
// * Geoapify Places API - Tìm địa điểm gần existing plans
// */
//interface GeoapifyService {
//
//    /**
//     * Search places by category near a location
//     * Categories: accommodation, restaurant, tourism, entertainment, etc.
//     */
//    @GET("v2/places")
//    suspend fun searchPlaces(
//        @Query("categories") categories: String,
//        @Query("filter") filter: String, // "circle:lng,lat,radius_meters"
//        @Query("bias") bias: String, // "proximity:lng,lat"
//        @Query("limit") limit: Int = 20,
//        @Query("apiKey") apiKey: String
//    ): Response<GeoapifyPlacesResponse>
//}
//
//// Response models for Geoapify API
//data class GeoapifyPlacesResponse(
//    val features: List<GeoapifyFeature>
//)
//
//data class GeoapifyFeature(
//    val properties: GeoapifyProperties,
//    val geometry: GeoapifyGeometry
//)
//
//data class GeoapifyProperties(
//    val name: String?,
//    val address_line1: String?,
//    val address_line2: String?,
//    val categories: List<String>?,
//    val distance: Double?
//)
//
//data class GeoapifyGeometry(
//    val coordinates: List<Double> // [lng, lat]
//)
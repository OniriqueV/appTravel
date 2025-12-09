package com.datn.apptravel.data.api

import com.datn.apptravel.data.model.response.GeoapifyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    

    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 20,
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyResponse>

    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("text") text: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 20,
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyResponse>

    @GET("v1/geocode/search")
    suspend fun geocodeSearch(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 1
    ): Response<GeoapifyResponse>
}
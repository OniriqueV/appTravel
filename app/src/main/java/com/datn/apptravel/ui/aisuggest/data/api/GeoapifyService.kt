package com.datn.apptravels.ui.aisuggest.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Models cho Geoapify
data class GeoapifyResponse(
    val features: List<Feature>?
)

data class Feature(
    val properties: Properties?
)

data class Properties(
    val formatted: String?,
    val city: String?,
    val country: String?,
    val name: String?
)

// Interface API
interface GeoapifyService {
    @GET("v1/geocode/autocomplete")
    suspend fun searchPlaces(
        @Query("text") query: String,
        @Query("apiKey") apiKey: String,
        @Query("filter") filter: String? = null, // Bỏ filter để tìm toàn cầu
        @Query("lang") lang: String = "vi",
        @Query("type") type: String = "city,country" // Chỉ tìm thành phố và quốc gia
    ): Response<GeoapifyResponse>
}

// Client
object GeoapifyClient {
    fun create(): GeoapifyService {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.geoapify.com/")
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        return retrofit.create(GeoapifyService::class.java)
    }
}
package com.datn.apptravels.ui.aisuggest.data.api

import com.datn.apptravels.ui.aisuggest.data.model.ChatRequest
import com.datn.apptravels.ui.aisuggest.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun generateItinerary(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

// API Client
object ApiClient {
    private const val BASE_URL = "https://api.groq.com/openai/"

    fun create(apiKey: String): ApiService {
        // Kiểm tra API key
        if (apiKey.isBlank() || apiKey == "\"\"") {
            throw IllegalStateException("API_KEY không hợp lệ. Vui lòng kiểm tra file local.properties")
        }

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
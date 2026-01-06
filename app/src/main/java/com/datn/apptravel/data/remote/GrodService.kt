package com.datn.apptravel.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


interface GroqService {

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: GroqRequest
    ): Response<GroqResponse>
}

// Request models
data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile", // Fast & powerful model
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 4000,
    val top_p: Double = 1.0,
    val stream: Boolean = false
)

data class GroqMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

// Response models
data class GroqResponse(
    val id: String?,

    val created: Long?,
    val model: String?,
    val choices: List<GroqChoice>?,
    val usage: GroqUsage?
)

data class GroqChoice(
    val index: Int?,
    val message: GroqMessage?,
    val finish_reason: String?
)

data class GroqUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)
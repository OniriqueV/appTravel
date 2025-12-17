package com.datn.apptravel.ui.aisuggest.data.model

import com.google.gson.annotations.SerializedName

// Request Models
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("max_tokens") val maxTokens: Int = 2000,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

// Response Models
data class ChatResponse(
    val id: String?,
    val choices: List<Choice>?,
    val error: ErrorResponse?
)

data class Choice(
    val message: Message?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ErrorResponse(
    val message: String?,
    val type: String?,
    val code: String?
)

// UI Models
data class TravelRequest(
    val destination: String,
    val days: Int,
    val budget: Long,
    val people: Int,
    val interests: List<String>
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
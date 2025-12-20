package com.datn.apptravel.ui.aisuggest.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Model lưu lịch trình
data class SavedItinerary(
    val id: String,
    val title: String,
    val destination: String,
    val days: Int,
    val budget: Long,
    val people: Int,
    val interests: List<String>,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Model tin nhắn chat
data class ChatMessage(
    val role: String, // "user" hoặc "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Helper cho JSON
object JsonHelper {
    val gson = Gson()

    fun <T> toJson(obj: T): String = gson.toJson(obj)

    inline fun <reified T> fromJson(json: String): T =
        gson.fromJson(json, object : TypeToken<T>() {}.type)
}
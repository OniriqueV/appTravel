package com.datn.apptravels.ui.discover.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

data class CommentDto(
    val id: Long,
    val planId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val parentId: String?, // For replies
    val content: String,
    @JsonAdapter(CreatedAtDeserializer::class)
    val createdAt: Long // Timestamp in milliseconds
)

/**
 * Custom deserializer to handle both formats:
 * 1. Long timestamp (e.g., 1767953560523)
 * 2. Firestore Timestamp object (e.g., {"seconds":1767953560,"nanos":523000000})
 */
class CreatedAtDeserializer : JsonDeserializer<Long> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Long {
        return when {
            json == null -> 0L
            json.isJsonPrimitive -> json.asLong
            json.isJsonObject -> {
                // Firestore Timestamp format: {"seconds": xxx, "nanos": yyy}
                val obj = json.asJsonObject
                val seconds = obj.get("seconds")?.asLong ?: 0L
                val nanos = obj.get("nanos")?.asLong ?: 0L
                // Convert to milliseconds
                seconds * 1000 + nanos / 1000000
            }
            else -> 0L
        }
    }
}

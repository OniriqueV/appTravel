package com.datn.apptravels.data.model

data class Document(
    val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val fileBase64: String = "", // Changed: Store Base64 string instead of URL
    val fileType: String = "", // "IMAGE", "PDF", "OTHER"
    val fileSize: Long = 0, // in bytes
    val category: String = "", // "TICKET", "BOOKING", "ITINERARY", "VISA", "OTHER"
    val description: String? = null,
    val tripId: String? = null, // Optional: link to specific trip
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", 0, "", null, null)

    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "fileName" to fileName,
            "fileBase64" to fileBase64,
            "fileType" to fileType,
            "fileSize" to fileSize,
            "category" to category,
            "description" to description,
            "tripId" to tripId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

enum class DocumentType {
    IMAGE, PDF, OTHER
}

enum class DocumentCategory {
    TICKET, BOOKING, ITINERARY, VISA, OTHER
}
package com.datn.apptravels.data.model

data class Document(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val fileName: String = "",
    val fileBase64: String = "", // Store Base64 string instead of URL
    val fileType: String = "", // "IMAGE", "PDF", "OTHER"
    val fileSize: Long = 0, // in bytes
    val category: String = "", // "TICKET", "BOOKING", "ITINERARY", "VISA", "OTHER"
    val description: String? = null,
    val tripId: String? = null, // Optional: link to specific trip
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "", 0, "", null, null, 0L, 0L)

    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,           // ← THÊM DÒNG NÀY!
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
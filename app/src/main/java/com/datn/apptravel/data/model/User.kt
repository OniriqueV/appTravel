package com.datn.apptravels.data.model

data class User(
    val id: String = "",                    // Firebase UID (String instead of Long)
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val role: String = "user",
    val profilePicture: String? = null,
    val provider: String? = null,           // "LOCAL" or "GOOGLE" (String for Firestore)
    val providerId: String? = null,         // Google ID, Facebook ID, etc.
    val enabled: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    constructor() : this("", "", "", "", "user")
    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "role" to role,
            "profilePicture" to profilePicture,
            "provider" to provider,
            "providerId" to providerId,
            "enabled" to enabled,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
}

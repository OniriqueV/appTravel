package com.datn.apptravel.data.model

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val profilePicture: String? = null,
    val provider: AuthProvider? = null,     // AuthProvider enum: LOCAL, GOOGLE
    val providerId: String? = null,         // Google ID, Facebook ID, etc.
    val enabled: Boolean = true,            // Account enabled status
    val createdAt: String? = null,
    val updatedAt: String? = null
)

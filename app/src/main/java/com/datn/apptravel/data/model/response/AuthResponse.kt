package com.datn.apptravels.data.model.response

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val userId: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String?
)
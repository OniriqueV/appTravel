package com.datn.apptravels.data.model.request

data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
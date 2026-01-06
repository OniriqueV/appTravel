package com.datn.apptravels.data.model.response

import com.datn.apptravels.data.model.Trip

data class TripResponse(
    val success: Boolean,
    val message: String?,
    val data: Trip?
)
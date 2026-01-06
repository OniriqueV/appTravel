package com.datn.apptravels.ui.trip.model

data class PlanLocation(
    val planId: String,
    val name: String,
    val time: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
    val iconResId: Int,
    val photoUrl: String? = null,
    var isHighlighted: Boolean = false
)
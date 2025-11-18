package com.datn.apptravel.ui.trip.model

data class PlanLocation(
    val name: String,
    val time: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
    val iconResId: Int,
    var isHighlighted: Boolean = false
)
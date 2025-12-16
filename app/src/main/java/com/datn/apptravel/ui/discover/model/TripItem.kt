package com.datn.apptravel.ui.discover.model

data class TripItem(
    val id: String,
    val title: String? = null,
    val coverPhoto: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

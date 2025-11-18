package com.datn.apptravel.ui.trip.model

import com.datn.apptravel.data.model.PlanType

data class ScheduleActivity(
    val id: Long = 0L,
    val time: String,
    val title: String,
    val description: String,
    val location: String? = null,
    val type: PlanType? = null,
    val expense: Double? = null,
    val photoUrl: String? = null,
    val iconResId: Int? = null
)
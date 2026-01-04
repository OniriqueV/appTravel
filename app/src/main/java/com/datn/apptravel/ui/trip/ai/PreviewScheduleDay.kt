package com.datn.apptravel.ui.trip.ai

import com.datn.apptravel.data.model.AISuggestedPlan

data class PreviewScheduleDay(
    val date: String, // yyyy-MM-dd
    val dayNumber: Int,
    val plans: MutableList<AISuggestedPlan>
)

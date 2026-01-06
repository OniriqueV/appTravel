package com.datn.apptravels.ui.trip.ai

import com.datn.apptravels.data.model.AISuggestedPlan

data class PreviewScheduleDay(
    val date: String, // yyyy-MM-dd
    val dayNumber: Int,
    val plans: MutableList<AISuggestedPlan>
)

package com.datn.apptravel.ui.trip.model

data class ScheduleDay(
    val dayNumber: Int,
    val title: String,
    val date: String,
    val activities: List<ScheduleActivity> = emptyList()  // Using ScheduleActivity for UI simplicity
)
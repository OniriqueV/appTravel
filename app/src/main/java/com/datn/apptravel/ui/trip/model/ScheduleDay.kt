package com.datn.apptravels.ui.trip.model

data class ScheduleDay(
    val dayNumber: Int,
    val title: String,
    val date: String,
    val activities: List<ScheduleActivity> = emptyList()
)
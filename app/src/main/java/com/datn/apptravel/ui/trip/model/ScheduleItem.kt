package com.datn.apptravel.ui.trip.model

sealed class ScheduleItem {
    data class DateItem(
        val label: String,  // "Start" or "End"
        val date: String    // formatted date
    ) : ScheduleItem()

    data class PlanItem(
        val plan: PlanLocation,
        val position: Int
    ) : ScheduleItem()

    data class ConnectorItem(
        val fromPlanPosition: Int,
        val toPlanPosition: Int
    ) : ScheduleItem()
}
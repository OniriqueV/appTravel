package com.datn.apptravels.data.model

data class UserStatistics(
    val userId: String = "",
    val totalTrips: Int = 0,
    val totalPlans: Int = 0,
    val totalExpense: Double = 0.0,
    val completedTrips: Int = 0,
    val upcomingTrips: Int = 0,
    val documentsUploaded: Int = 0,
    val plansByType: Map<String, Int> = emptyMap(), // e.g., {"ACTIVITY": 10, "LODGING": 5}
    val lastUpdated: Long = System.currentTimeMillis()
) {
    constructor() : this("", 0, 0, 0.0, 0, 0, 0, emptyMap())

    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "userId" to userId,
            "totalTrips" to totalTrips,
            "totalPlans" to totalPlans,
            "totalExpense" to totalExpense,
            "completedTrips" to completedTrips,
            "upcomingTrips" to upcomingTrips,
            "documentsUploaded" to documentsUploaded,
            "plansByType" to plansByType,
            "lastUpdated" to lastUpdated
        )
    }
}
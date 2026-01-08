package com.datn.apptravels.data.repository

import android.util.Log
import com.datn.apptravels.data.model.UserStatistics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsRepository(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "StatisticsRepository"
    private val tripsCollection = firestore.collection("trips")
    private val plansCollection = firestore.collection("plans")
    private val documentsCollection = firestore.collection("documents")
    private val statisticsCollection = firestore.collection("statistics")

    // All plan types in the app - matching PlanType enum
    private val ALL_PLAN_TYPES = listOf(
        "NONE",
        "LODGING",
        "FLIGHT",
        "RESTAURANT",
        "TOUR",
        "BOAT",
        "TRAIN",
        "RELIGION",
        "CAR_RENTAL",
        "CAMPING",
        "THEATER",
        "SHOPPING",
        "ACTIVITY"
    )

    suspend fun calculateUserStatistics(userId: String): Result<UserStatistics> {
        return try {
            Log.d(TAG, "Calculating statistics for user: $userId")

            // Get all user trips
            val tripsSnapshot = tripsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val trips = tripsSnapshot.documents
            Log.d(TAG, "Found ${trips.size} trips in Firestore")

            // Get trip IDs
            val tripIds = trips.map { it.id }
            Log.d(TAG, "Trip IDs: $tripIds")

            // Calculate total trips
            val totalTrips = trips.size

            // Calculate trip status with 3 states
            var completedTrips = 0
            var ongoingTrips = 0
            var upcomingTrips = 0

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStartOfDay = calendar.timeInMillis

            Log.d(TAG, "Today (start of day): ${dateFormat.format(todayStartOfDay)}")

            trips.forEach { tripDoc ->
                val startDateStr = tripDoc.getString("startDate")
                val endDateStr = tripDoc.getString("endDate")

                Log.d(TAG, "Trip ${tripDoc.id}: start=$startDateStr, end=$endDateStr")

                if (startDateStr != null && endDateStr != null) {
                    try {
                        val startDate = dateFormat.parse(startDateStr)?.time ?: 0
                        val endDate = dateFormat.parse(endDateStr)?.time ?: 0

                        when {
                            endDate < todayStartOfDay -> {
                                // End date is before today -> Completed
                                completedTrips++
                                Log.d(TAG, "Trip ${tripDoc.id}: COMPLETED")
                            }
                            startDate > todayStartOfDay -> {
                                // Start date is after today -> Upcoming
                                upcomingTrips++
                                Log.d(TAG, "Trip ${tripDoc.id}: UPCOMING")
                            }
                            else -> {
                                // Today is between start and end -> Ongoing
                                ongoingTrips++
                                Log.d(TAG, "Trip ${tripDoc.id}: ONGOING")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing dates for trip ${tripDoc.id}: ${e.message}")
                        upcomingTrips++
                    }
                } else {
                    Log.w(TAG, "Trip ${tripDoc.id} missing dates")
                    upcomingTrips++
                }
            }

            // Initialize plansByType with all types = 0
            val plansByType = mutableMapOf<String, Int>()
            ALL_PLAN_TYPES.forEach { type ->
                plansByType[type] = 0
            }

            // Get ALL plans for user's trips
            var totalPlans = 0
            var totalExpense = 0.0

            if (tripIds.isNotEmpty()) {
                // Firestore 'in' query supports max 10 items
                val batches = tripIds.chunked(10)

                for (batch in batches) {
                    val plansSnapshot = plansCollection
                        .whereIn("tripId", batch)
                        .get()
                        .await()

                    Log.d(TAG, "Found ${plansSnapshot.size()} plans for batch")

                    plansSnapshot.documents.forEach { planDoc ->
                        totalPlans++

                        // Get plan type
                        val planType = planDoc.getString("type") ?: "OTHER"

                        // Increment count for this type
                        plansByType[planType] = (plansByType[planType] ?: 0) + 1

                        // Get expense
                        val expense = planDoc.getDouble("expense") ?: 0.0
                        totalExpense += expense

                        Log.d(TAG, "Plan ${planDoc.id}: type=$planType, expense=$expense")
                    }
                }
            }

            // Get documents count
            val documentsSnapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val documentsUploaded = documentsSnapshot.size()

            Log.d(TAG, "=== FINAL STATISTICS ===")
            Log.d(TAG, "Total trips: $totalTrips")
            Log.d(TAG, "Completed trips: $completedTrips")
            Log.d(TAG, "Ongoing trips: $ongoingTrips")
            Log.d(TAG, "Upcoming trips: $upcomingTrips")
            Log.d(TAG, "Total plans: $totalPlans")
            Log.d(TAG, "Total expense: $totalExpense VND")
            Log.d(TAG, "Documents: $documentsUploaded")
            Log.d(TAG, "Plans by type:")
            plansByType.forEach { (type, count) ->
                Log.d(TAG, "  $type: $count")
            }

            // Create statistics object
            val statistics = UserStatistics(
                userId = userId,
                totalTrips = totalTrips,
                totalPlans = totalPlans,
                totalExpense = totalExpense,
                completedTrips = completedTrips,
                ongoingTrips = ongoingTrips,
                upcomingTrips = upcomingTrips,
                documentsUploaded = documentsUploaded,
                plansByType = plansByType,
                lastUpdated = System.currentTimeMillis()
            )

            // Save to Firestore for caching
            statisticsCollection.document(userId).set(statistics.toMap()).await()

            Result.success(statistics)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating statistics: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserStatistics(userId: String): Result<UserStatistics> {
        return try {
            val snapshot = statisticsCollection.document(userId).get().await()
            val statistics = snapshot.toObject(UserStatistics::class.java)

            if (statistics != null) {
                Log.d(TAG, "Loaded cached statistics")
                Result.success(statistics)
            } else {
                Log.d(TAG, "No cached statistics, calculating...")
                calculateUserStatistics(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting statistics: ${e.message}", e)
            Result.failure(e)
        }
    }
}
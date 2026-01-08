package com.datn.apptravels.data.repository

import com.datn.apptravels.data.model.UserStatistics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class StatisticsRepository(
    private val firestore: FirebaseFirestore
) {
    private val tripsCollection = firestore.collection("trips")
    private val documentsCollection = firestore.collection("documents")
    private val statisticsCollection = firestore.collection("statistics")

    suspend fun calculateUserStatistics(userId: String): Result<UserStatistics> {
        return try {
            // Get all user trips
            val tripsSnapshot = tripsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val trips = tripsSnapshot.documents

            // Calculate total trips
            val totalTrips = trips.size

            // Calculate total plans and expenses
            var totalPlans = 0
            var totalExpense = 0.0
            val plansByType = mutableMapOf<String, Int>()
            var completedTrips = 0
            var upcomingTrips = 0

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = System.currentTimeMillis()

            trips.forEach { tripDoc ->
                val plans = tripDoc.get("plans") as? List<*>

                plans?.forEach { planObj ->
                    totalPlans++

                    // Count by plan type
                    val planMap = planObj as? Map<*, *>
                    val planType = planMap?.get("type") as? String ?: "OTHER"
                    plansByType[planType] = (plansByType[planType] ?: 0) + 1

                    // Sum expenses
                    val expense = (planMap?.get("expense") as? Number)?.toDouble() ?: 0.0
                    totalExpense += expense
                }

                // Check trip status
                val endDateStr = tripDoc.getString("endDate")
                if (endDateStr != null) {
                    try {
                        val endDate = dateFormat.parse(endDateStr)?.time ?: 0
                        if (endDate < today) {
                            completedTrips++
                        } else {
                            upcomingTrips++
                        }
                    } catch (e: Exception) {
                        // Ignore parse errors
                    }
                }
            }

            // Get documents count
            val documentsSnapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val documentsUploaded = documentsSnapshot.size()

            // Create statistics object
            val statistics = UserStatistics(
                userId = userId,
                totalTrips = totalTrips,
                totalPlans = totalPlans,
                totalExpense = totalExpense,
                completedTrips = completedTrips,
                upcomingTrips = upcomingTrips,
                documentsUploaded = documentsUploaded,
                plansByType = plansByType,
                lastUpdated = System.currentTimeMillis()
            )

            // Save to Firestore for caching
            statisticsCollection.document(userId).set(statistics.toMap()).await()

            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStatistics(userId: String): Result<UserStatistics> {
        return try {
            val snapshot = statisticsCollection.document(userId).get().await()
            val statistics = snapshot.toObject(UserStatistics::class.java)

            if (statistics != null) {
                Result.success(statistics)
            } else {
                // If no cached statistics, calculate new ones
                calculateUserStatistics(userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
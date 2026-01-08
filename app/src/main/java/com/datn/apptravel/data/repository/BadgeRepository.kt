package com.datn.apptravels.data.repository

import com.datn.apptravels.data.model.Badge
import com.datn.apptravels.data.model.BadgeCondition
import com.datn.apptravels.data.model.BadgeDefinitions
import com.datn.apptravels.data.model.UserBadge
import com.datn.apptravels.data.model.UserStatistics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BadgeRepository(
    private val firestore: FirebaseFirestore
) {
    private val userBadgesCollection = firestore.collection("userBadges")

    suspend fun getUserBadges(userId: String): Result<List<UserBadge>> {
        return try {
            val snapshot = userBadgesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val badges = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserBadge::class.java)
            }

            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAndAwardBadges(
        userId: String,
        statistics: UserStatistics
    ): Result<List<UserBadge>> {
        return try {
            val allBadges = BadgeDefinitions.getAllBadges()
            val currentBadges = getUserBadges(userId).getOrNull() ?: emptyList()
            val currentBadgeIds = currentBadges.map { it.badgeId }.toSet()

            val newBadges = mutableListOf<UserBadge>()

            allBadges.forEach { badge ->
                // Skip if user already has this badge
                if (currentBadgeIds.contains(badge.id)) {
                    return@forEach
                }

                // Check if user meets the condition
                val meetsCondition = when (badge.condition) {
                    BadgeCondition.TRIPS_CREATED ->
                        statistics.totalTrips >= badge.threshold
                    BadgeCondition.PLANS_COMPLETED ->
                        statistics.totalPlans >= badge.threshold
                    BadgeCondition.DOCUMENTS_UPLOADED ->
                        statistics.documentsUploaded >= badge.threshold
                    BadgeCondition.DAYS_TRAVELED ->
                        false // Not implemented yet
                    BadgeCondition.COUNTRIES_VISITED ->
                        false // Not implemented yet
                }

                if (meetsCondition) {
                    val userBadge = UserBadge(
                        userId = userId,
                        badgeId = badge.id,
                        badgeName = badge.name,
                        badgeDescription = badge.description,
                        badgeIconRes = badge.iconRes,
                        earnedAt = System.currentTimeMillis(),
                        isNew = true
                    )

                    // Save to Firestore
                    val docRef = userBadgesCollection.document()
                    docRef.set(userBadge.toMap()).await()

                    newBadges.add(userBadge)
                }
            }

            Result.success(newBadges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markBadgeAsViewed(userId: String, badgeId: String): Result<Unit> {
        return try {
            val snapshot = userBadgesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("badgeId", badgeId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.update("isNew", false).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllAvailableBadges(): List<Badge> {
        return BadgeDefinitions.getAllBadges()
    }
}
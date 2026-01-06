package com.datn.apptravels.data.repository

import android.util.Log
import com.datn.apptravels.data.api.RetrofitClient
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.data.model.Notification
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(private val sessionManager: SessionManager) {

    private val auth = FirebaseAuth.getInstance()
    private val apiService = RetrofitClient.notificationApiService

    companion object {
        private const val TAG = "NotificationRepository"
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun saveNotification(notification: Notification): Result<Unit> {
        return try {

            Log.d(TAG, " Notification saved by Backend via FCM")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save notification", e)
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val userId = getCurrentUserId()
            
            if (userId == null) {
                Log.e(TAG, "User not logged in - cannot get notifications")
                return Result.failure(Exception("User not logged in"))
            }
            
            Log.d(TAG, "Fetching notifications from API for userId: $userId")

            val response = withContext(Dispatchers.IO) {
                apiService.getUserNotifications(userId)
            }

            if (response.isSuccessful) {
                val notifications = response.body() ?: emptyList()
                Log.d(TAG, "Got ${notifications.size} notifications from API")
                Result.success(notifications)
            } else {
                Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("API error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notifications: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.markAsRead(notificationId)
            }

            if (response.isSuccessful) {
                Log.d(TAG, "Marked notification as read: $notificationId")
                Result.success(Unit)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.failure(Exception("API error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification as read", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.deleteNotification(notificationId)
            }

            if (response.isSuccessful) {
                Log.d(TAG, "Deleted notification: $notificationId")
                Result.success(Unit)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.failure(Exception("API error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification", e)
            Result.failure(e)
        }
    }

    suspend fun saveNotificationSettings(enabled: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

            // Save to SessionManager first (immediate UI update)
            sessionManager.saveNotificationsEnabled(enabled)
            
            val response = withContext(Dispatchers.IO) {
                apiService.updateNotificationSettings(userId, mapOf("enabled" to enabled))
            }

            if (response.isSuccessful) {
                Log.d(TAG, "Saved notification settings: enabled=$enabled")
                Result.success(Unit)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.failure(Exception("API error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, " Failed to save notification settings", e)
            Result.failure(e)
        }
    }

    suspend fun getNotificationSettings(): Result<Boolean> {
        return try {
            // Return from SessionManager cache (instant)
            val cachedValue = sessionManager.getNotificationsEnabled()
            Log.d(TAG, "Loaded notification settings from cache: enabled=$cachedValue")
            Result.success(cachedValue)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get notification settings", e)
            Result.success(true) // Default to enabled
        }
    }
    
    suspend fun loadNotificationSettingsFromServer(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

            val response = withContext(Dispatchers.IO) {
                apiService.getUserDevice(userId)
            }

            if (response.isSuccessful) {
                val enabled = (response.body()?.get("notificationsEnabled") as? Boolean) ?: true
                Log.d(TAG, "Loaded notification settings from server: enabled=$enabled")
                
                // Cache to SessionManager
                sessionManager.saveNotificationsEnabled(enabled)
                
                Result.success(enabled)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.success(true) // Default to enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load notification settings from server", e)
            Result.success(true) // Default to enabled
        }
    }

    suspend fun saveFcmToken(token: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

            val response = withContext(Dispatchers.IO) {
                apiService.saveFcmToken(mapOf(
                    "userId" to userId,
                    "fcmToken" to token
                ))
            }

            if (response.isSuccessful) {
                Log.d(TAG, "Saved FCM token for user: $userId")
                Result.success(Unit)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.failure(Exception("API error: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

            val response = withContext(Dispatchers.IO) {
                apiService.getUnreadCount(userId)
            }

            if (response.isSuccessful) {
                val count = response.body()?.get("count") ?: 0
                Result.success(count)
            } else {
                Log.e(TAG, "API error: ${response.code()}")
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unread count", e)
            Result.success(0)
        }
    }
}

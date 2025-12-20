package com.datn.apptravel

import android.app.Application
import android.util.Log
import com.datn.apptravel.data.repository.NotificationRepository
import com.datn.apptravel.di.appModule
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AppTravelApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationRepository: NotificationRepository by inject()
    
    companion object {
        private const val TAG = "AppTravelApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin dependency injection
        startKoin {
            androidContext(this@AppTravelApplication)
            modules(appModule)
        }
        
        // Load notification settings from server (once at app startup)
        loadNotificationSettings()
        
        // Subscribe to FCM topics for notifications
        subscribeToNotificationTopics()
        
        // Get FCM token for debugging
        getFCMToken()
    }
    
    private fun loadNotificationSettings() {
        applicationScope.launch {
            try {
                val result = notificationRepository.loadNotificationSettingsFromServer()
                result.onSuccess { enabled ->
                    Log.d(TAG, "Notification settings loaded at startup: enabled=$enabled")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load notification settings: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification settings", e)
            }
        }
    }
    
    private fun subscribeToNotificationTopics() {
        // Subscribe to trip reminders topic
        FirebaseMessaging.getInstance().subscribeToTopic("trip-reminders")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to trip-reminders topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to trip-reminders", task.exception)
                }
            }
        
        // Subscribe to test notifications topic (for testing)
        FirebaseMessaging.getInstance().subscribeToTopic("test-notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to test-notifications topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to test-notifications", task.exception)
                }
            }
    }
    
    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, " FCM Token: $token")
                // TODO: Send this token to your backend server
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }
}
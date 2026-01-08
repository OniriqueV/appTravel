package com.datn.apptravels

import android.app.Application
import android.util.Log
import com.datn.apptravels.data.repository.NotificationRepository
import com.datn.apptravels.di.appModule
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

        startKoin {
            androidContext(this@AppTravelApplication)
            modules(appModule)
        }

        loadNotificationSettings()
        subscribeToNotificationTopics()
        getFCMToken()
    }

    private fun loadNotificationSettings() {
        applicationScope.launch {
            try {
                val result = notificationRepository.loadNotificationSettingsFromServer()
                result.onSuccess { enabled ->
                    Log.d(TAG, "Notification settings loaded: enabled=$enabled")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load notification settings: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notification settings", e)
            }
        }
    }

    private fun subscribeToNotificationTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("trip-reminders")
        FirebaseMessaging.getInstance().subscribeToTopic("test-notifications")
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token obtained: $token")
                
                // Save token to backend
                applicationScope.launch {
                    try {
                        val result = notificationRepository.saveFcmToken(token)
                        result.onSuccess {
                            Log.d(TAG, "FCM token saved to backend successfully")
                        }.onFailure { error ->
                            Log.e(TAG, " Failed to save FCM token: ${error.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving FCM token", e)
                    }
                }
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }
}

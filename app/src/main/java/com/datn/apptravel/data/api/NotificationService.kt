package com.datn.apptravel.data.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.datn.apptravel.R
import com.datn.apptravel.data.model.Notification
import com.datn.apptravel.data.model.NotificationType
import com.datn.apptravel.data.repository.NotificationRepository
import com.datn.apptravel.ui.activity.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "trip_notifications"
        private const val CHANNEL_NAME = "Trip Notifications"
    }

    private val notificationRepository: NotificationRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")

        // Save token to Firestore
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains notification payload
        message.notification?.let { notification ->
            val title = notification.title ?: "Thông báo"
            val body = notification.body ?: ""
            val imageUrl = notification.imageUrl?.toString()

            Log.d(TAG, "Notification: title=$title, body=$body")

            // Save notification to Firestore (always save)
            saveNotificationLocally(title, body, message.data)

            // Check user settings before showing notification
            CoroutineScope(Dispatchers.IO).launch {
                val settingsResult = notificationRepository.getNotificationSettings()
                val isEnabled = settingsResult.getOrDefault(true)
                
                if (isEnabled) {
                    Log.d(TAG, " Notifications enabled - showing notification")
                    showNotification(title, body, imageUrl, message.data)
                } else {
                    Log.d(TAG, "Notifications disabled - notification saved but not shown")
                }
            }
        }

        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataPayload(message.data)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        imageUrl: String?,
        data: Map<String, String>
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for trip reminders and updates"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveNotificationLocally(title: String, body: String, data: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    id = System.currentTimeMillis().toString(),
                    title = title,
                    message = body,
                    type = when (data["type"]) {
                        "TRIP_REMINDER" -> NotificationType.TRIP
                        else -> NotificationType.GENERAL
                    },
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                // Save to Firestore
                notificationRepository.saveNotification(notification)

                // Broadcast notification update
                sendBroadcast(Intent("com.datn.apptravel.NOTIFICATION_RECEIVED"))

                Log.d(TAG, " Saved notification to Firestore: $title")
            } catch (e: Exception) {
                Log.e(TAG, " Failed to save notification", e)
            }
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "TRIP_REMINDER" -> {
                val tripId = data["tripId"]
                val tripTitle = data["tripTitle"]
                Log.d(TAG, "Trip reminder: $tripTitle (ID: $tripId)")
            }

            "DAILY_SUMMARY" -> {
                val tripId = data["tripId"]
                val tripCount = data["tripCount"]
                Log.d(TAG, "Daily summary: $tripCount trips")
            }

            "TRIP_UPDATE" -> {
                val tripId = data["tripId"]
                Log.d(TAG, "Trip updated: $tripId")
            }

            else -> {
                Log.d(TAG, "Unknown notification type: $type")
            }
        }
    }
}

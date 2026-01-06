package com.datn.apptravels.data.api

import com.datn.apptravels.data.model.Notification
import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {
    
    @GET("api/notifications/user/{userId}")
    suspend fun getUserNotifications(
        @Path("userId") userId: String
    ): Response<List<Notification>>
    
    @PUT("api/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: String
    ): Response<Map<String, String>>
    
    @DELETE("api/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: String
    ): Response<Map<String, String>>
    
    @GET("api/notifications/user/{userId}/unread-count")
    suspend fun getUnreadCount(
        @Path("userId") userId: String
    ): Response<Map<String, Int>>
    
    @POST("api/notifications/device/token")
    suspend fun saveFcmToken(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>
    
    @PUT("api/notifications/settings/{userId}")
    suspend fun updateNotificationSettings(
        @Path("userId") userId: String,
        @Body request: Map<String, Boolean>
    ): Response<Map<String, String>>
    
    @GET("api/notifications/device/{userId}")
    suspend fun getUserDevice(
        @Path("userId") userId: String
    ): Response<Map<String, Any>>
}

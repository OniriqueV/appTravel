package com.datn.apptravel.data.model

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false
)

enum class NotificationType {
    FLIGHT,      // Chuyến bay
    ACTIVITY,    // Hoạt động/Gợi ý
    TRIP,        // Chuyến đi
    REMINDER,    // Nhắc nhở
    GENERAL      // Thông báo chung
}

package com.datn.apptravels.data.model

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType?,  // Nullable to handle parsing errors
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

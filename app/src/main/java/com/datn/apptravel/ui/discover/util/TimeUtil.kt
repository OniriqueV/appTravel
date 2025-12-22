package com.datn.apptravel.ui.discover.util

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.time.Duration


object TimeUtil {

    fun formatTimeAgo(isoTime: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                java.util.Locale.getDefault()
            )
            sdf.timeZone = java.util.TimeZone.getDefault()

            val time = sdf.parse(isoTime) ?: return ""
            val now = System.currentTimeMillis()
            val diff = now - time.time

            val minutes = diff / (60 * 1000)

            when {
                minutes < 1 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                minutes < 1440 -> "${minutes / 60} giờ trước"
                else -> "${minutes / 1440} ngày trước"
            }
        } catch (e: Exception) {
            ""
        }
    }
}

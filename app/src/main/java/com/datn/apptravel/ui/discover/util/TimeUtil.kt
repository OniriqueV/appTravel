package com.datn.apptravel.ui.discover.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date


object TimeUtil {

    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS"
    )

    fun formatTimeAgo(isoTime: String?): String {
        if (isoTime.isNullOrBlank()) return ""

        val date = parseIsoDate(isoTime) ?: return ""

        val now = System.currentTimeMillis()
        val diffMillis = now - date.time

        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 7 -> "$days ngày trước"
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(date)
            }
        }
    }

    private fun parseIsoDate(value: String): Date? {
        val cleaned = value.substringBefore('.') // cắt nano

        for (pattern in isoFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC") // 🔥 FIX
                return sdf.parse(cleaned)
            } catch (_: Exception) {
            }
        }
        return null
    }
}

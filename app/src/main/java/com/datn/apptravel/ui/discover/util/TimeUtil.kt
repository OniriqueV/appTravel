package com.datn.apptravel.ui.discover.util

import java.util.concurrent.TimeUnit

object TimeUtil {

    fun formatTimeAgo(timeMillis: Long?): String {
        if (timeMillis == null || timeMillis == 0L) return ""

        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Vừa xong"
            diff < TimeUnit.HOURS.toMillis(1) ->
                "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
            diff < TimeUnit.DAYS.toMillis(1) ->
                "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
            diff < TimeUnit.DAYS.toMillis(7) ->
                "${TimeUnit.MILLISECONDS.toDays(diff)} ngày trước"
            else -> "Lâu rồi"
        }
    }
}

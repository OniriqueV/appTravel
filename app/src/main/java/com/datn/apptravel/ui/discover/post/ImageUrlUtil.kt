package com.datn.apptravel.ui.discover.post

import com.datn.apptravel.BuildConfig

object ImageUrlUtil {

    fun toFullUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()

        // đã là link full
        if (s.startsWith("http://") || s.startsWith("https://")) return s

        val baseHost = BuildConfig.TRIP_SERVICE_BASE_URL.trimEnd('/')

        return when {
            // "/uploads/abc.jpg"  -> "http://10.0.2.2:8080/uploads/abc.jpg"
            s.startsWith("/uploads/") -> baseHost + s

            // "uploads/abc.jpg" -> "http://10.0.2.2:8080/uploads/abc.jpg"
            s.startsWith("uploads/") -> "$baseHost/$s"

            // "/abc.jpg" fallback
            s.startsWith("/") -> baseHost + s

            // "abc.jpg" -> ".../uploads/abc.jpg"
            else -> BuildConfig.UPLOAD_BASE_URL + s
        }
    }
}

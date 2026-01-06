package com.datn.apptravels.utils

import com.datn.apptravels.BuildConfig

object ApiConfig {

    val TRIP_SERVICE_BASE_URL: String = BuildConfig.TRIP_SERVICE_BASE_URL

    val UPLOAD_BASE_URL: String = BuildConfig.UPLOAD_BASE_URL
    fun getImageUrl(fileName: String?): String? {
        return if (!fileName.isNullOrEmpty()) {
            UPLOAD_BASE_URL + fileName
        } else {
            null
        }
    }
}

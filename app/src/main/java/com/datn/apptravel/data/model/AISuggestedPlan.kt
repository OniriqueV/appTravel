package com.datn.apptravels.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Model độc lập backend - chỉ dùng cho UI và Intent
 * Chuẩn hóa dữ liệu từ AI để redirect đúng Activity
 */
@Parcelize
data class AISuggestedPlan(
    val id: String = "",
    val type: PlanType,
    val title: String,
    val address: String,
    val lat: Double,
    val lng: Double,

    val startTime: String, // ISO: 2024-12-30T14:00:00


    val expense: Double? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val notes: String? = null,

    val isSelected: Boolean = false
) : Parcelable



/**
 * User insights từ Q&A dialog
 */
@Parcelize
data class UserInsight(
    val budget: String? = null, // "LOW", "MEDIUM", "HIGH"
    val travelStyle: String? = null, // "RELAXED", "BALANCED", "PACKED"
    val interests: List<String> = emptyList(), // ["FOOD", "CULTURE", "NATURE"]
    val groupSize: Int? = null,
    val hasChildren: Boolean = false,
    val preferredCuisine: List<String> = emptyList(),
    val accommodationType: String? = null // "HOTEL", "HOSTEL", "APARTMENT"
) : Parcelable

/**
 * Response từ Geoapify Places API
 */
data class GeoapifyPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val categories: List<String> = emptyList(),
    val distance: Double? = null
)

/**
 * Response từ Google Custom Search API
 */
data class GoogleImageResult(
    val link: String,
    val thumbnailLink: String? = null
)

/**
 * Internal model for analyzed plans (used in AIRepository)
 */
data class AnalyzedPlan(
    val name: String,
    val lat: Double,
    val lng: Double,
    val type: String
)
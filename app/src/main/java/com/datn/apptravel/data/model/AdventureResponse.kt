package com.datn.apptravels.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Response model for Adventure endpoint
 * Contains all trip and user data in a single response
 */
data class AdventureResponse(
    @SerializedName("items")
    val items: List<AdventureItem>
)

data class AdventureItem(
    @SerializedName("tripId")
    val tripId: String,
    
    @SerializedName("trip")
    val trip: TripDetail,
    
    @SerializedName("user")
    val user: UserDetail,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("startDateText")
    val startDateText: String,
    
    @SerializedName("durationText")
    val durationText: String
)

data class TripDetail(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("startDate")
    val startDate: String,
    
    @SerializedName("endDate")
    val endDate: String,
    
    @SerializedName("isPublic")
    val isPublic: String?,
    
    @SerializedName("coverPhoto")
    val coverPhoto: String?,
    
    @SerializedName("content")
    val content: String?,
    
    @SerializedName("tags")
    val tags: String?,
    
    @SerializedName("plans")
    val plans: List<Plan>?,
    
    @SerializedName("members")
    val members: List<User>?,
    
    @SerializedName("sharedWithUsers")
    val sharedWithUsers: List<User>?,
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("sharedAt")
    val sharedAt: String?
)

data class UserDetail(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("firstName")
    val firstName: String,
    
    @SerializedName("lastName")
    val lastName: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("profilePicture")
    val profilePicture: String?,
    
    @SerializedName("role")
    val role: String?,
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: String?
)

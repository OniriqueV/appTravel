package com.datn.apptravels.data.model

import com.datn.apptravels.ui.discover.model.CommentDto

open class Plan(
    open val id: String? = null,
    open val tripId: String,
    open val title: String,
    open val address: String? = null,
    open val location: String? = null,
    open val startTime: String,          //  format: yyyy-MM-dd'T'HH:mm:ss
    open val expense: Double? = null,
    open val photoUrl: String? = null,
    open val photos: List<String>? = null,  // Collection of photos (filenames)
    open val type: PlanType,
    open val likesCount: Int = 0,        // Simplified - count instead of full list
    open val commentsCount: Int = 0,     // Simplified - count instead of full list
    open val comments: List<CommentDto>? = null,  // Full list of comments from backend
    open val createdAt: String? = null,
    
    // ActivityPlan specific field
    open val endTime: String? = null,    // For ActivityPlan: end time of activity
    
    // LodgingPlan specific fields
    open val checkInDate: String? = null,
    open val checkOutDate: String? = null,
    
    // RestaurantPlan specific fields
    open val reservationDate: String? = null,
    open val reservationTime: String? = null,
    
    // FlightPlan specific fields
    open val arrivalLocation: String? = null,
    open val arrivalAddress: String? = null,
    open val arrivalDate: String? = null,
    
    // BoatPlan specific fields
    open val arrivalTime: String? = null,
    
    // CarRentalPlan specific fields
    open val pickupDate: String? = null,
    open val pickupTime: String? = null,
    
    // Common phone field (used by Lodging, CarRental, Restaurant)
    open val phone: String? = null
)
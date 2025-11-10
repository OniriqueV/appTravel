package com.datn.apptravel.data.model.mapper

import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.ui.model.ScheduleActivity
import com.datn.apptravel.ui.model.PlanLocation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object ModelMapper {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val isoFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    /**
     * Convert Plan entity to ScheduleActivity for UI display
     */
    fun Plan.toScheduleActivity(): ScheduleActivity {
        val displayTime = try {
            val dateTime = LocalDateTime.parse(this.startTime, isoFormatter)
            dateTime.format(timeFormatter)
        } catch (e: Exception) {
            this.startTime // Fallback to original string
        }
        
        return ScheduleActivity(
            id = this.id,
            time = displayTime,
            title = this.title,
            description = this.location ?: "",
            location = this.location,
            type = this.type,
            expense = this.expense,
            photoUrl = this.photoUrl,
            iconResId = this.type.iconRes
        )
    }
    
    /**
     * Convert Plan entity to PlanLocation for map display
     */
    fun Plan.toPlanLocation(
        latitude: Double,
        longitude: Double,
        isHighlighted: Boolean = false
    ): PlanLocation {
        val displayTime = try {
            val dateTime = LocalDateTime.parse(this.startTime, isoFormatter)
            dateTime.format(timeFormatter)
        } catch (e: Exception) {
            this.startTime
        }
        
        return PlanLocation(
            name = this.title,
            time = displayTime,
            detail = this.location ?: "",
            latitude = latitude,
            longitude = longitude,
            iconResId = this.type.iconRes,
            isHighlighted = isHighlighted
        )
    }
    
    /**
     * Format datetime string to display time (HH:mm a)
     */
    fun String.toDisplayTime(): String {
        return try {
            val dateTime = LocalDateTime.parse(this, isoFormatter)
            dateTime.format(timeFormatter)
        } catch (e: Exception) {
            this // Return original if parsing fails
        }
    }
    
    /**
     * Format datetime string to display date (yyyy-MM-dd)
     */
    fun String.toDisplayDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(this, isoFormatter)
            dateTime.format(DateTimeFormatter.ISO_DATE)
        } catch (e: Exception) {
            this
        }
    }
}

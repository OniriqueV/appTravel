package com.datn.apptravels.data.model

data class CityPlan(
    val cityName: String,
    val startDate: String,
    val endDate: String,
    val budget: Long? = null,  // Ngân sách cho city này (VNĐ)
    val numberOfPlans: Int? = null,  // Số lượng plans mong muốn

    val userNotes: String? = null
)

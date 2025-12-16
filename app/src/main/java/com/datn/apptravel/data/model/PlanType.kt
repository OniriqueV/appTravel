package com.datn.apptravel.data.model

import androidx.annotation.DrawableRes
import com.datn.apptravel.R

enum class PlanType(
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val geoapifyCategory: String
) {
    NONE("None", R.drawable.ic_globe, ""),
    LODGING("Lodging", R.drawable.ic_lodgingsss, "accommodation"),
    FLIGHT("Flight", R.drawable.ic_flight, "airport"),
    RESTAURANT("Restaurant", R.drawable.ic_restaurant, "catering.restaurant"),
    TOUR("Tour", R.drawable.ic_toursss, "tourism.attraction"),
    BOAT("Boat", R.drawable.ic_boat, "rental.boat"),
    TRAIN("Train", R.drawable.ic_train, "railway.subway"),
    RELIGION("Religion", R.drawable.ic_religion, "religion"),
    CAR_RENTAL("Car Rental", R.drawable.ic_car, "rental.car"),
    CAMPING("Camping", R.drawable.ic_location, "camping.camp_site"),
    THEATER("Theater", R.drawable.ic_theater, "entertainment.culture.theatre"),
    SHOPPING("Shopping", R.drawable.ic_shopping, "commercial.shopping_mall"),
    ACTIVITY("Activity", R.drawable.ic_attraction, "leisure");
    
    companion object {
        fun getDefaultTypes(): List<PlanType> {
            return listOf(NONE, LODGING, FLIGHT)
        }
    }
}

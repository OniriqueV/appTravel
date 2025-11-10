package com.datn.apptravel.util

import com.datn.apptravel.R
import com.datn.apptravel.ui.model.PlanLocation

object TripPlanManager {
    
    // Sample plans for Paris trip (tọa độ thật ở Paris)
    fun getParisPlans(): List<PlanLocation> {
        return listOf(
            PlanLocation(
                name = "Hotel Check-in",
                time = "08:00",
                detail = "Le Marais Hotel",
                latitude = 48.8566,
                longitude = 2.3522,
                iconResId = R.drawable.ic_lodging
            ),
            PlanLocation(
                name = "Breakfast",
                time = "09:30",
                detail = "Café de Flore",
                latitude = 48.8542,
                longitude = 2.3324,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Eiffel Tower",
                time = "11:00",
                detail = "Visit & Photo",
                latitude = 48.8584,
                longitude = 2.2945,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Lunch",
                time = "13:30",
                detail = "Le Jules Verne",
                latitude = 48.8579,
                longitude = 2.2952,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Louvre Museum",
                time = "15:00",
                detail = "Art Tour",
                latitude = 48.8606,
                longitude = 2.3376,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Seine River Cruise",
                time = "18:00",
                detail = "Sunset Cruise",
                latitude = 48.8635,
                longitude = 2.3366,
                iconResId = R.drawable.ic_boat
            ),
            PlanLocation(
                name = "Dinner",
                time = "20:00",
                detail = "Le Comptoir",
                latitude = 48.8525,
                longitude = 2.3390,
                iconResId = R.drawable.ic_restaurant
            )
        )
    }
    
    // Sample plans for Barrier Reef trip (tọa độ thật ở Cairns, Australia)
    fun getBarrierReefPlans(): List<PlanLocation> {
        return listOf(
            PlanLocation(
                name = "Hotel Check-in",
                time = "07:00",
                detail = "Cairns Beach Resort",
                latitude = -16.9186,
                longitude = 145.7781,
                iconResId = R.drawable.ic_lodging
            ),
            PlanLocation(
                name = "Breakfast",
                time = "08:00",
                detail = "Waterfront Café",
                latitude = -16.9203,
                longitude = 145.7710,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Marina Departure",
                time = "09:00",
                detail = "Reef Fleet Terminal",
                latitude = -16.9225,
                longitude = 145.7817,
                iconResId = R.drawable.ic_boat
            ),
            PlanLocation(
                name = "Green Island",
                time = "11:00",
                detail = "Snorkeling",
                latitude = -16.7617,
                longitude = 145.9739,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Lunch on Boat",
                time = "13:00",
                detail = "Reef Cruise",
                latitude = -16.8500,
                longitude = 145.9000,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Diving Spot",
                time = "15:00",
                detail = "Outer Reef",
                latitude = -16.9000,
                longitude = 146.0500,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Return to Marina",
                time = "17:00",
                detail = "Cairns Port",
                latitude = -16.9225,
                longitude = 145.7817,
                iconResId = R.drawable.ic_boat
            )
        )
    }
    
    // Sample plans for Tokyo trip (tọa độ thật ở Tokyo)
    fun getTokyoPlans(): List<PlanLocation> {
        return listOf(
            PlanLocation(
                name = "Hotel Check-in",
                time = "08:00",
                detail = "Shibuya Excel Hotel",
                latitude = 35.6580,
                longitude = 139.7016,
                iconResId = R.drawable.ic_lodging
            ),
            PlanLocation(
                name = "Breakfast",
                time = "09:00",
                detail = "Ichiran Ramen",
                latitude = 35.6620,
                longitude = 139.7005,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Senso-ji Temple",
                time = "10:30",
                detail = "Asakusa Temple",
                latitude = 35.7148,
                longitude = 139.7967,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Tokyo Skytree",
                time = "13:00",
                detail = "Observatory Visit",
                latitude = 35.7101,
                longitude = 139.8107,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Lunch",
                time = "15:00",
                detail = "Sushi Saito",
                latitude = 35.6655,
                longitude = 139.7295,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Shibuya Crossing",
                time = "17:00",
                detail = "Shopping & Photos",
                latitude = 35.6595,
                longitude = 139.7004,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Dinner",
                time = "19:00",
                detail = "Yakitori Alley",
                latitude = 35.6938,
                longitude = 139.7036,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Tokyo Tower",
                time = "21:00",
                detail = "Night View",
                latitude = 35.6586,
                longitude = 139.7454,
                iconResId = R.drawable.ic_location
            )
        )
    }
    
    // Sample plans for Vietnam trip (Hanoi -> Ha Long Bay -> Hue -> Ho Chi Minh)
    fun getVietnamPlans(): List<PlanLocation> {
        return listOf(
            PlanLocation(
                name = "Noi Bai Airport",
                time = "08:00",
                detail = "Arrival Hanoi",
                latitude = 21.2212,
                longitude = 105.8070,
                iconResId = R.drawable.ic_flight
            ),
            PlanLocation(
                name = "Hotel Check-in",
                time = "10:00",
                detail = "Old Quarter Hotel",
                latitude = 21.0285,
                longitude = 105.8542,
                iconResId = R.drawable.ic_lodging
            ),
            PlanLocation(
                name = "Hoan Kiem Lake",
                time = "11:00",
                detail = "Walking Tour",
                latitude = 21.0285,
                longitude = 105.8522,
                iconResId = R.drawable.ic_location
            ),
            PlanLocation(
                name = "Pho 24",
                time = "12:30",
                detail = "Vietnamese Lunch",
                latitude = 21.0278,
                longitude = 105.8513,
                iconResId = R.drawable.ic_restaurant
            ),
            PlanLocation(
                name = "Ha Long Bay",
                time = "15:00",
                detail = "Cruise Tour",
                latitude = 20.9101,
                longitude = 107.1839,
                iconResId = R.drawable.ic_boat
            ),
            PlanLocation(
                name = "Sunset Point",
                time = "18:00",
                detail = "Titop Island",
                latitude = 20.8450,
                longitude = 107.0850,
                iconResId = R.drawable.ic_location
            )
        )
    }
    
    fun getPlansByTripId(tripId: String, tripTitle: String): List<PlanLocation> {
        return when {
            tripTitle.contains("Paris", ignoreCase = true) -> getParisPlans()
            tripTitle.contains("Barrier Reef", ignoreCase = true) || 
                tripTitle.contains("Australia", ignoreCase = true) -> getBarrierReefPlans()
            tripTitle.contains("Tokyo", ignoreCase = true) || 
                tripTitle.contains("Japan", ignoreCase = true) -> getTokyoPlans()
            tripTitle.contains("Vietnam", ignoreCase = true) -> getVietnamPlans()
            else -> getParisPlans() // Default
        }
    }
}

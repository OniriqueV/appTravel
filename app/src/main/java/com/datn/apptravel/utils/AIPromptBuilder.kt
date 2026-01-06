package com.datn.apptravels.utils

import com.datn.apptravels.data.model.AnalyzedPlan
import com.datn.apptravels.data.model.GeoapifyPlace
import com.datn.apptravels.data.model.UserInsight

object AIPromptBuilder {

    fun buildPrompt(
        startDate: String,
        endDate: String,
        existingPlans: List<AnalyzedPlan>,
        nearbyPlaces: List<GeoapifyPlace>,
        userInsight: UserInsight?
    ): String {
        val sb = StringBuilder()

        sb.appendLine("You are a travel planning AI assistant. Generate travel plan suggestions based on the following information:")
        sb.appendLine()

        // Trip dates
        sb.appendLine("TRIP DATES:")
        sb.appendLine("- Start: $startDate")
        sb.appendLine("- End: $endDate")
        sb.appendLine()

        // Existing plans
        if (existingPlans.isNotEmpty()) {
            sb.appendLine("EXISTING PLANS:")
            existingPlans.forEachIndexed { index, plan ->
                sb.appendLine("${index + 1}. ${plan.name} (${plan.type})")
                sb.appendLine("   Location: ${plan.lat}, ${plan.lng}")
            }
            sb.appendLine()
            sb.appendLine("IMPORTANT: Generate suggestions that are geographically close to these existing plans (within 5-10km radius).")
            sb.appendLine()
        }

        // Nearby places from Geoapify
        if (nearbyPlaces.isNotEmpty()) {
            sb.appendLine("NEARBY PLACES (from Geoapify):")
            nearbyPlaces.take(15).forEachIndexed { index, place ->
                sb.appendLine("${index + 1}. ${place.name}")
                sb.appendLine("   Address: ${place.address}")
                sb.appendLine("   Location: ${place.lat}, ${place.lon}")
                sb.appendLine("   Categories: ${place.categories.joinToString()}")
                if (place.distance != null) {
                    sb.appendLine("   Distance: ${String.format("%.0f", place.distance)}m from center")
                }
            }
            sb.appendLine()
            sb.appendLine("IMPORTANT: Prioritize these nearby places in your suggestions.")
            sb.appendLine()
        }

        // User insights
        if (userInsight != null) {
            sb.appendLine("USER PREFERENCES:")
            userInsight.budget?.let { sb.appendLine("- Budget: $it") }
            userInsight.travelStyle?.let { sb.appendLine("- Travel style: $it") }
            if (userInsight.interests.isNotEmpty()) {
                sb.appendLine("- Interests: ${userInsight.interests.joinToString()}")
            }
            userInsight.groupSize?.let { sb.appendLine("- Group size: $it people") }
            if (userInsight.hasChildren) {
                sb.appendLine("- Traveling with children: Yes")
            }
            if (userInsight.preferredCuisine.isNotEmpty()) {
                sb.appendLine("- Preferred cuisine: ${userInsight.preferredCuisine.joinToString()}")
            }
            userInsight.accommodationType?.let { sb.appendLine("- Accommodation type: $it") }
            sb.appendLine()
        }

        // Instructions
        sb.appendLine("TASK:")
        sb.appendLine("Generate 6-10 diverse travel plan suggestions that:")
        sb.appendLine("1. Are geographically close to existing plans (if any)")
        sb.appendLine("2. Fill gaps in the itinerary (meals, activities, rest)")
        sb.appendLine("3. Match user preferences (if provided)")
        sb.appendLine("4. Are realistic and practical")
        sb.appendLine("5. Include accurate coordinates and addresses")
        sb.appendLine()

        sb.appendLine("PLAN TYPES:")
        sb.appendLine("- RESTAURANT: Dining options (breakfast, lunch, dinner, cafe)")
        sb.appendLine("- LODGING: Hotels, hostels, apartments")
        sb.appendLine("- ACTIVITY: Museums, parks, attractions, tours")
        sb.appendLine("- SHOPPING: Markets, malls, souvenir shops")
        sb.appendLine("- ENTERTAINMENT: Shows, theaters, nightlife")
        sb.appendLine("- TOUR: Guided tours, day trips")
        sb.appendLine()

        sb.appendLine("OUTPUT FORMAT:")
        sb.appendLine("Return ONLY a valid JSON array with this exact structure:")
        sb.appendLine("""
[
  {
    "type": "RESTAURANT",
    "title": "Exact place name",
    "address": "Full address",
    "lat": 10.762622,
    "lng": 106.660172,
    "startTime": "2024-12-30T12:00:00",
    "expense": 150000,
    "description": "Brief description",
    "notes": "Optional notes"
  }
]
        """.trimIndent())
        sb.appendLine()

        sb.appendLine("CRITICAL RULES:")
        sb.appendLine("1. Use REAL locations from the nearby places list when possible")
        sb.appendLine("2. Ensure coordinates are accurate and close to existing plans")
        sb.appendLine("3. Times should be within trip dates and logical (breakfast at 8am, dinner at 7pm)")
        sb.appendLine("4. Expenses in VND (Vietnamese Dong)")
        sb.appendLine("5. NO markdown, NO explanations - ONLY the JSON array")
        sb.appendLine("6. Spread suggestions across different days of the trip")

        return sb.toString()
    }
}
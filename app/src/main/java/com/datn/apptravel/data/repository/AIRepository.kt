package com.datn.apptravels.data.repository

import android.util.Log
import com.datn.apptravels.BuildConfig
import com.datn.apptravels.data.model.*
import com.datn.apptravels.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AIRepository(
    private val groqService: GroqService,
    private val imageSearchRepository: ImageSearchRepository
) {
    private val TAG = "AIRepository"

    // API Keys
    private val GROQ_API_KEY = BuildConfig.GROQ_API_KEY


    private suspend fun callGroqAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling Groq API...")

                val request = GroqRequest(
                    model = "llama-3.3-70b-versatile", // Fast model
                    messages = listOf(
                        GroqMessage(
                            role = "user",
                            content = prompt
                        )
                    ),
                    temperature = 0.7,
                    max_tokens = 4000
                )

                val response = groqService.chatCompletion(
                    authorization = "Bearer $GROQ_API_KEY",
                    request = request
                )

                if (response.isSuccessful && response.body() != null) {
                    val choices = response.body()?.choices
                    if (!choices.isNullOrEmpty()) {
                        val content = choices[0].message?.content
                        if (content != null) {
                            Log.d(TAG, "Groq API response received successfully")
                            return@withContext content
                        }
                    }
                    throw Exception("No valid response from Groq API")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Groq API error: $errorBody")
                    throw Exception("Groq API call failed: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Groq API", e)
                throw e
            }
        }
    }

    private fun parseAIResponse(response: String, startDate: String, endDate: String): List<AISuggestedPlan> {
        val suggestions = mutableListOf<AISuggestedPlan>()

        try {
            Log.d(TAG, "Parsing AI response...")

            // Clean response - remove markdown code blocks if present
            var cleanedResponse = response.trim()
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.removePrefix("```json").removeSuffix("```").trim()
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.removePrefix("```").removeSuffix("```").trim()
            }

            // Find JSON array in response
            val jsonStart = cleanedResponse.indexOf('[')
            val jsonEnd = cleanedResponse.lastIndexOf(']')

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleanedResponse = cleanedResponse.substring(jsonStart, jsonEnd + 1)
            }

            Log.d(TAG, "Cleaned response: ${cleanedResponse.take(200)}...")

            // Parse JSON array
            val plansArray = JSONArray(cleanedResponse)

            for (j in 0 until plansArray.length()) {
                val plan = plansArray.getJSONObject(j)

                try {
                    val typeString = plan.getString("type")

                    suggestions.add(AISuggestedPlan(
                        id = "ai_${System.currentTimeMillis()}_$j",
                        type = PlanType.fromString(typeString),
                        title = plan.getString("title"),
                        address = plan.getString("address"),
                        lat = plan.getDouble("lat"),
                        lng = plan.getDouble("lng"),
                        startTime = plan.getString("startTime"),
                        expense = if (plan.has("expense") && !plan.isNull("expense")) {
                            plan.getDouble("expense")
                        } else null,
                        description = if (plan.has("description") && !plan.isNull("description")) {
                            plan.getString("description")
                        } else null,
                        notes = if (plan.has("notes") && !plan.isNull("notes")) {
                            plan.getString("notes")
                        } else null
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing individual plan at index $j", e)
                }
            }

            Log.d(TAG, "Successfully parsed ${suggestions.size} suggestions")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            Log.e(TAG, "Response was: ${response.take(500)}")
        }

        return suggestions
    }

    private suspend fun enrichWithImages(suggestions: List<AISuggestedPlan>): List<AISuggestedPlan> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting to enrich ${suggestions.size} plans with images")
            suggestions.map { plan ->
                async {
                    try {
                        Log.d(TAG, "Fetching image for: ${plan.title}")
                        // Use ImageSearchRepository to fetch the first image
                        val imageUrls = imageSearchRepository.searchImages(
                            query = "${plan.title} ${plan.address}",
                            count = 1
                        )

                        if (imageUrls.isNotEmpty()) {
                            Log.d(TAG, "✓ Found image for ${plan.title}: ${imageUrls[0]}")
                            plan.copy(photoUrl = imageUrls[0])
                        } else {
                            Log.w(TAG, "✗ No image found for ${plan.title}")
                            plan
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Error fetching image for ${plan.title}: ${e.message}", e)
                        plan
                    }
                }
            }.map { it.await() }
        }
    }

    suspend fun generatePlansForMultipleCities(
        cities: List<CityPlan>
    ): Result<List<AISuggestedPlan>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating AI plans for ${cities.size} cities")

            // Build multi-city prompt with transportation
            val prompt = buildMultiCityPrompt(cities)

            // Call Groq AI API
            val aiResponse = callGroqAPI(prompt)

            // Parse AI response
            val suggestions = parseAIResponse(aiResponse, cities.first().startDate, cities.last().endDate)
            Log.d(TAG, "Parsed ${suggestions.size} suggestions from AI")

            // Enrich with images (optional)
            val enrichedSuggestions = try {
                Log.d(TAG, "Starting image enrichment...")
                val result = enrichWithImages(suggestions)
                Log.d(TAG, "Image enrichment completed. Plans with images: ${result.count { !it.photoUrl.isNullOrEmpty() }}")
                result
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enrich with images: ${e.message}", e)
                suggestions
            }

            Log.d(TAG, "Generated ${enrichedSuggestions.size} plans for multiple cities")
            Result.success(enrichedSuggestions)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating plans for multiple cities", e)
            Result.failure(e)
        }
    }

    /**
     * Build AI prompt for multiple cities with transportation
     */
    private fun buildMultiCityPrompt(cities: List<CityPlan>): String {
        // Check if any city has budget or numberOfPlans constraints
        val hasBudgetConstraints = cities.any { it.budget != null }
        val hasPlansConstraints = cities.any { it.numberOfPlans != null }
        
        val citiesDescription = cities.mapIndexed { index, city ->
            val budgetInfo = if (city.budget != null) {
                "\n   ⚠️ STRICT BUDGET LIMIT: ${city.budget} VND - TOTAL expenses MUST be LESS than this"
            } else if (hasBudgetConstraints) {
                "\n   💰 Budget: Flexible (use mid-range prices ~2-3M VND total)"
            } else ""
            
            val plansInfo = if (city.numberOfPlans != null) {
                "\n   ⚠️ EXACT COUNT REQUIRED: ${city.numberOfPlans} plans - NO MORE, NO LESS"
            } else if (hasPlansConstraints) {
                "\n   📋 Plans count: Flexible (8-12 plans)"
            } else ""
            
            "${index + 1}. ${city.cityName}: ${city.startDate} to ${city.endDate}$budgetInfo$plansInfo"
        }.joinToString("\n\n")

        return """
You are a travel AI. Generate itinerary for multi-city trip.

═══════════════════════════════════════
CITIES WITH CONSTRAINTS (STRICT RULES):
═══════════════════════════════════════
$citiesDescription

═══════════════════════════════════════
⚠️ MANDATORY RULES - FAILURE = REJECTED:
═══════════════════════════════════════

${if (hasBudgetConstraints) """
🔴 BUDGET RULE (NON-NEGOTIABLE):
For cities with budget specified:
1. SUM ALL expenses: lodging + all restaurants + all activities + tours
2. TOTAL MUST BE < specified budget (leave 15% safety margin)
3. Example: Budget 5,000,000 VND
   ✓ CORRECT: Hotel 1,500,000 + Meals 1,800,000 + Activities 1,200,000 = 4,500,000 ✓
   ✗ WRONG: Hotel 2,500,000 + Meals 2,000,000 + Activities 1,500,000 = 6,000,000 ✗
4. Price guidelines:
   - Budget <3M: Hotel 500-800k/night, meals 50-150k each, free activities
   - Budget 3-7M: Hotel 800k-1.5M/night, meals 150-300k each, paid activities
   - Budget >7M: Luxury options OK

For cities WITHOUT budget: Use reasonable mid-range prices (total ~2-3M)

""" else ""}
${if (hasPlansConstraints) """
🔴 PLANS COUNT RULE (NON-NEGOTIABLE):
For cities with numberOfPlans specified:
1. Generate EXACTLY that number - count every plan you create
2. Example: numberOfPlans=10 → count: 1 hotel + 6 restaurants + 3 activities = 10 ✓
3. DO NOT round up or down - EXACT number only
4. Include: 1 hotel + meals (breakfast/lunch/dinner) + activities/tours

For cities WITHOUT numberOfPlans: Generate 8-12 plans based on duration

""" else ""}
🟡 PLAN TYPES per city:
- LODGING: 1 hotel (entire stay)
- RESTAURANT: 2-3 meals/day (breakfast 50-150k, lunch 100-250k, dinner 150-400k)
- ACTIVITY: Attractions, museums (0-500k each)
- TOUR: Optional guided tours (200k-1M each)

🟡 TRANSPORTATION between cities (separate from city budgets):
- FLIGHT: >300km or islands
- TRAIN: 100-300km
- CAR_RENTAL: Road trips
- BOAT: Coastal areas

🟡 TIME FORMAT: "yyyy-MM-ddTHH:mm:ss"
- Morning: 08:00-12:00
- Afternoon: 13:00-17:00  
- Evening: 18:00-22:00

═══════════════════════════════════════
✅ PRE-SUBMISSION CHECKLIST (MANDATORY):
═══════════════════════════════════════
${if (hasPlansConstraints) """
1. Count plans for each city → Verify = numberOfPlans EXACTLY
""" else ""}
${if (hasBudgetConstraints) """
${if (hasPlansConstraints) "2" else "1"}. Calculate SUM of expenses per city → Verify < budget
""" else ""}
${if (hasPlansConstraints || hasBudgetConstraints) 
    "${if (hasPlansConstraints && hasBudgetConstraints) "3" else "2"}." else "1."} All fields complete (type, title, address, lat, lng, startTime, expense, description)
${if (hasPlansConstraints || hasBudgetConstraints) 
    "${if (hasPlansConstraints && hasBudgetConstraints) "4" else "3"}." else "2."} Valid JSON array format

═══════════════════════════════════════
RESPONSE FORMAT (ONLY JSON, NO TEXT):
═══════════════════════════════════════
[
  {
    "type": "LODGING",
    "title": "Hotel name",
    "address": "Street, Ward, District, City",
    "lat": 10.123456,
    "lng": 106.123456,
    "startTime": "2026-01-16T14:00:00",
    "expense": 1200000,
    "description": "3-star hotel with breakfast"
  },
  {
    "type": "RESTAURANT",
    "title": "Restaurant name",
    "address": "Full address",
    "lat": 10.123456,
    "lng": 106.123456,
    "startTime": "2026-01-16T18:00:00",
    "expense": 250000,
    "description": "Vietnamese cuisine"
  }
]

Valid types: LODGING, RESTAURANT, ACTIVITY, TOUR, FLIGHT, TRAIN, BOAT, CAR_RENTAL, THEATER, SHOPPING, CAMPING, RELIGION

⚠️ VERIFY BUDGET + PLANS COUNT BEFORE RETURNING
Return ONLY JSON array - NO explanations, NO markdown, NO extra text.
    "description": "What to do"
  },
  {
    "type": "FLIGHT",
    "title": "Flight from City 1 to City 2",
    "address": "Departure airport to Arrival airport",
    "lat": departure_lat,
    "lng": departure_lng,
    "startTime": "yyyy-MM-ddTHH:mm:ss",
    "expense": price_in_VND,
    "description": "Flight details"
  }
]

Valid types: LODGING, RESTAURANT, ACTIVITY, TOUR, FLIGHT, BOAT, TRAIN, CAR_RENTAL, CAMPING, THEATER, SHOPPING, RELIGION

Generate comprehensive plans covering all cities with appropriate transportation. Return ONLY the JSON array.
""".trimIndent()
    }
}
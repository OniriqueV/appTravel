package com.datn.apptravel.data.repository

import android.util.Log
import com.datn.apptravel.BuildConfig
import com.datn.apptravel.data.model.*
import com.datn.apptravel.data.remote.*
import com.datn.apptravel.utils.AIPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AIRepository(
    private val geoapifyService: GeoapifyService,
    private val googleImageService: GoogleImageService,
    private val groqService: GroqService
) {
    private val TAG = "AIRepository"

    // API Keys
    private val GEOAPIFY_KEY = BuildConfig.GEOAPIFY_KEY
    private val GOOGLE_API_KEY = BuildConfig.GOOGLE_API_KEY
    private val GOOGLE_CX = BuildConfig.GOOGLE_CX
    private val GROQ_API_KEY = BuildConfig.GROQ_API_KEY

    /**
     * Main function: Generate AI suggestions
     */
    suspend fun generateAISuggestions(
        tripId: String,
        startDate: String,
        endDate: String,
        existingPlans: List<Plan>,
        userInsight: UserInsight?
    ): Result<List<AISuggestedPlan>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating AI suggestions for trip: $tripId")

            // Step 1: Phân tích existing plans
            val analyzedPlans = analyzeExistingPlans(existingPlans)
            Log.d(TAG, "Analyzed ${analyzedPlans.size} existing plans")

            // Step 2: Tìm địa điểm gần existing plans qua Geoapify
            val nearbyPlaces = if (analyzedPlans.isNotEmpty()) {
                findNearbyPlaces(analyzedPlans)
            } else {
                emptyList()
            }
            Log.d(TAG, "Found ${nearbyPlaces.size} nearby places")

            // Step 3: Build AI prompt
            val prompt = AIPromptBuilder.buildPrompt(
                startDate = startDate,
                endDate = endDate,
                existingPlans = analyzedPlans,
                nearbyPlaces = nearbyPlaces,
                userInsight = userInsight
            )

            // Step 4: Call Groq AI API (FAST!)
            val aiResponse = callGroqAPI(prompt)

            // Step 5: Parse AI response to AISuggestedPlan
            val suggestions = parseAIResponse(aiResponse, startDate, endDate)

            // Step 6: Enrich với Google Images (optional, có thể bỏ qua nếu lỗi)
            val enrichedSuggestions = try {
                enrichWithImages(suggestions)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enrich with images, using suggestions without images", e)
                suggestions
            }

            Log.d(TAG, "Generated ${enrichedSuggestions.size} suggestions")
            Result.success(enrichedSuggestions)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating suggestions", e)
            Result.failure(e)
        }
    }

    /**
     * Phân tích existing plans để lấy thông tin location
     */
    private fun analyzeExistingPlans(plans: List<Plan>): List<AnalyzedPlan> {
        val analyzed = mutableListOf<AnalyzedPlan>()

        plans.forEach { plan ->
            try {
                val locationStr = plan.location
                if (!locationStr.isNullOrBlank()) {
                    val parts = locationStr.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].trim().toDoubleOrNull()
                        val lng = parts[1].trim().toDoubleOrNull()

                        if (lat != null && lng != null) {
                            analyzed.add(
                                AnalyzedPlan(
                                    name = plan.title ?: "Unknown",
                                    lat = lat,
                                    lng = lng,
                                    type = plan.type?.name ?: "OTHER"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing plan: ${plan.title}", e)
            }
        }

        return analyzed
    }

    /**
     * Tìm nearby places qua Geoapify
     */
    private suspend fun findNearbyPlaces(existingPlans: List<AnalyzedPlan>): List<GeoapifyPlace> {
        if (existingPlans.isEmpty()) return emptyList()

        // Lấy center point từ existing plans
        val centerLat = existingPlans.map { it.lat }.average()
        val centerLng = existingPlans.map { it.lng }.average()

        val places = mutableListOf<GeoapifyPlace>()

        try {
            // Search different categories
            val categories = listOf(
                "accommodation",
                "catering.restaurant",
                "tourism.attraction",
                "entertainment"
            )

            categories.forEach { category ->
                try {
                    val response = geoapifyService.searchPlaces(
                        categories = category,
                        filter = "circle:$centerLng,$centerLat,5000", // 5km radius
                        bias = "proximity:$centerLng,$centerLat",
                        limit = 10,
                        apiKey = GEOAPIFY_KEY
                    )

                    if (response.isSuccessful) {
                        response.body()?.features?.forEach { feature ->
                            places.add(GeoapifyPlace(
                                name = feature.properties.name ?: "Unknown",
                                address = feature.properties.address_line1 ?: "",
                                lat = feature.geometry.coordinates[1],
                                lon = feature.geometry.coordinates[0],
                                categories = feature.properties.categories ?: emptyList(),
                                distance = feature.properties.distance
                            ))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching category: $category", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearby places", e)
        }

        return places.take(20) // Limit to 20 places
    }

    /**
     * Call Groq AI API - EXTREMELY FAST!
     */
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

    /**
     * Parse AI response to AISuggestedPlan list
     */
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

    /**
     * Enrich suggestions with Google Images (optional)
     */
    private suspend fun enrichWithImages(suggestions: List<AISuggestedPlan>): List<AISuggestedPlan> {
        return withContext(Dispatchers.IO) {
            suggestions.map { plan ->
                async {
                    try {
                        val response = googleImageService.searchImages(
                            query = "${plan.title} ${plan.address}",
                            apiKey = GOOGLE_API_KEY,
                            searchEngineId = GOOGLE_CX
                        )

                        if (response.isSuccessful) {
                            val imageUrl = response.body()?.items?.firstOrNull()?.link
                            plan.copy(photoUrl = imageUrl)
                        } else {
                            plan
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching image for ${plan.title}", e)
                        plan
                    }
                }
            }.map { it.await() }
        }
    }
}
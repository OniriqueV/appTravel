package com.datn.apptravel.ui.trip.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.R
import com.datn.apptravel.data.api.OSRMRetrofitClient
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import com.datn.apptravel.ui.trip.model.PlanLocation
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TripMapViewModel(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    private val _planLocations = MutableLiveData<List<PlanLocation>>()
    val planLocations: LiveData<List<PlanLocation>> = _planLocations

    private val _tripDates = MutableLiveData<Pair<String, String>>()
    val tripDates: LiveData<Pair<String, String>> = _tripDates

    private val _routeSegments = MutableLiveData<List<List<GeoPoint>>>()
    val routeSegments: LiveData<List<List<GeoPoint>>> = _routeSegments

    private val _centerLocation = MutableLiveData<GeoPoint>()
    val centerLocation: LiveData<GeoPoint> = _centerLocation

    private val _routeLoadStatus = MutableLiveData<RouteLoadStatus>()
    val routeLoadStatus: LiveData<RouteLoadStatus> = _routeLoadStatus

    data class RouteLoadStatus(
        val successful: Int,
        val failed: Int,
        val total: Int
    )

    fun loadTripData(tripId: String, packageName: String, currentUserId: String? = null, tripUserId: String? = null) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Load trip to get start/end dates
                tripRepository.getTripById(tripId).onSuccess { trip ->
                    _tripDates.value = Pair(trip.startDate, trip.endDate)
                }

                // Load plans from API
                tripRepository.getPlansByTripId(tripId).onSuccess { apiPlans ->
                    if (apiPlans.isEmpty()) {
                        setError("No plans found for this trip")
                        _planLocations.value = emptyList()
                        setLoading(false)
                        return@onSuccess
                    }

                    // Filter plans based on ownership
                    val filteredPlans = if (currentUserId != null && tripUserId != null && currentUserId != tripUserId) {
                        // User is not owner - only show plans that have already occurred
                        val today = java.time.LocalDate.now()
                        apiPlans.filter { plan ->
                            try {
                                val planDateTime = LocalDateTime.parse(
                                    plan.startTime,
                                    DateTimeFormatter.ISO_DATE_TIME
                                )
                                val planDate = planDateTime.toLocalDate()
                                // Show plans from today or before (planDate <= today)
                                !planDate.isAfter(today)
                            } catch (e: Exception) {
                                Log.e("TripMapViewModel", "Error parsing date for plan: ${plan.title}", e)
                                false // Don't show plans with invalid dates
                            }
                        }
                    } else {
                        // User is owner or no user info - show all plans
                        apiPlans
                    }

                    // Convert API plans to PlanLocation with geocoding
                    val planLocations = convertPlansToLocations(filteredPlans, packageName)
                    _planLocations.value = planLocations

                    if (planLocations.isNotEmpty()) {
                        _centerLocation.value = GeoPoint(
                            planLocations[0].latitude,
                            planLocations[0].longitude
                        )
                    }
                }.onFailure { error ->
                    setError("Failed to load plans: ${error.message}")
                    Log.e("TripMapViewModel", "Error loading plans", error)
                }

                setLoading(false)
            } catch (e: Exception) {
                setLoading(false)
                setError("Error: ${e.message}")
                Log.e("TripMapViewModel", "Error in loadTripData", e)
            }
        }
    }

    private suspend fun convertPlansToLocations(
        apiPlans: List<Plan>,
        packageName: String
    ): List<PlanLocation> {
        return withContext(Dispatchers.IO) {
            apiPlans.mapNotNull { plan ->
                try {
                    // Parse time from ISO format
                    val time = try {
                        val dateTime = LocalDateTime.parse(
                            plan.startTime,
                            DateTimeFormatter.ISO_DATE_TIME
                        )
                        String.format("%02d:%02d", dateTime.hour, dateTime.minute)
                    } catch (e: Exception) {
                        "00:00"
                    }

                    // Get coordinates from location field (format: "latitude,longitude")
                    val locationStr = plan.location
                    val coordinates = if (!locationStr.isNullOrBlank()) {
                        try {
                            val parts = locationStr.split(",")
                            if (parts.size == 2) {
                                Pair(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                            } else null
                        } catch (e: Exception) {
                            Log.e("TripMapViewModel", "Error parsing location: $locationStr", e)
                            null
                        }
                    } else {
                        // Fallback: geocode from address if location is not available
                        geocodeLocation(plan.address ?: plan.title, packageName)
                    }

                    if (coordinates != null) {
                        PlanLocation(
                            name = plan.title,
                            time = time,
                            detail = plan.address ?: "",
                            latitude = coordinates.first,
                            longitude = coordinates.second,
                            iconResId = getIconForPlanType(plan.type),
                            photoUrl = plan.photoUrl
                        )
                    } else {
                        Log.w("TripMapViewModel", "Could not get coordinates for: ${plan.title}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("TripMapViewModel", "Error converting plan: ${plan.title}", e)
                    null
                }
            }
        }
    }

    private suspend fun geocodeLocation(
        address: String,
        packageName: String
    ): Pair<Double, Double>? {
        return try {
            // Use Nominatim (OpenStreetMap) geocoding API
            val response = withContext(Dispatchers.IO) {
                val baseUrl = com.datn.apptravel.BuildConfig.NOMINATIM_BASE_URL
                val url = "${baseUrl}search?q=${
                    URLEncoder.encode(address, "UTF-8")
                }&format=json&limit=1"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", packageName)
                connection.connect()

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                responseText
            }

            // Parse JSON response
            val jsonArray = Gson().fromJson(response, JsonArray::class.java)

            if (jsonArray.size() > 0) {
                val firstResult = jsonArray[0].asJsonObject
                val lat = firstResult.get("lat").asDouble
                val lon = firstResult.get("lon").asDouble
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TripMapViewModel", "Geocoding error for: $address", e)
            null
        }
    }

    private fun getIconForPlanType(planType: PlanType): Int {
        return when (planType) {
            PlanType.LODGING -> R.drawable.ic_lodgingsss
            PlanType.RESTAURANT -> R.drawable.ic_restaurant
            PlanType.FLIGHT -> R.drawable.ic_flight
            PlanType.CAR_RENTAL -> R.drawable.ic_car
            PlanType.TRAIN -> R.drawable.ic_train
            PlanType.BOAT -> R.drawable.ic_boat
            PlanType.TOUR -> R.drawable.ic_toursss
            PlanType.ACTIVITY -> R.drawable.ic_attraction
            PlanType.THEATER -> R.drawable.ic_theater
            PlanType.SHOPPING -> R.drawable.ic_shopping
            PlanType.CAMPING -> R.drawable.ic_location
            PlanType.RELIGION -> R.drawable.ic_religion
            PlanType.NONE -> R.drawable.ic_globe
        }
    }

    fun drawRoute(plans: List<PlanLocation>) {
        if (plans.size < 2) {
            Log.w("TripMapViewModel", "drawRoute: Not enough plans (${plans.size})")
            return
        }

        Log.d("TripMapViewModel", "drawRoute: Starting to draw route for ${plans.size} plans")
        setLoading(true)

        viewModelScope.launch {
            val segments = mutableListOf<List<GeoPoint>>()
            var successfulSegments = 0
            var failedSegments = 0

            try {
                // Draw each segment between consecutive plans separately
                for (i in 0 until plans.size - 1) {
                    val fromPlan = plans[i]
                    val toPlan = plans[i + 1]

                    Log.d("TripMapViewModel", "Drawing segment $i: ${fromPlan.name} -> ${toPlan.name}")

                    // Build coordinates string for this segment only
                    val coordinates =
                        "${fromPlan.longitude},${fromPlan.latitude};${toPlan.longitude},${toPlan.latitude}"

                    try {
                        val response = withContext(Dispatchers.IO) {
                            OSRMRetrofitClient.apiService.getRoute(
                                coordinates = coordinates,
                                overview = "full",
                                geometries = "geojson",
                                steps = true
                            )
                        }

                        if (response.isSuccessful && response.body()?.code == "Ok") {
                            val route = response.body()?.routes?.firstOrNull()
                            if (route != null) {
                                Log.d("TripMapViewModel", "Got route geometry for segment $i")
                                // Parse GeoJSON geometry
                                val geoPoints = parseGeoJSONToPoints(route.geometry)
                                if (geoPoints.isNotEmpty()) {
                                    segments.add(geoPoints)
                                    successfulSegments++
                                } else {
                                    segments.add(createStraightSegment(fromPlan, toPlan))
                                    failedSegments++
                                }
                            } else {
                                segments.add(createStraightSegment(fromPlan, toPlan))
                                failedSegments++
                            }
                        } else {
                            Log.w("TripMapViewModel", "OSRM request failed for segment $i")
                            segments.add(createStraightSegment(fromPlan, toPlan))
                            failedSegments++
                        }
                    } catch (segmentException: Exception) {
                        Log.w("TripMapViewModel", "Segment $i failed: ${segmentException.message}")
                        segments.add(createStraightSegment(fromPlan, toPlan))
                        failedSegments++
                    }
                }

                _routeSegments.value = segments
                _routeLoadStatus.value = RouteLoadStatus(
                    successful = successfulSegments,
                    failed = failedSegments,
                    total = plans.size - 1
                )

                Log.d("TripMapViewModel", "drawRoute: Completed with $successfulSegments successful, $failedSegments failed segments")
            } catch (e: Exception) {
                Log.e("TripMapViewModel", "Fatal error in drawRoute: ${e.message}", e)
                setError("Network error loading routes")
                // Fallback: create all straight segments
                val straightSegments = mutableListOf<List<GeoPoint>>()
                for (i in 0 until plans.size - 1) {
                    straightSegments.add(createStraightSegment(plans[i], plans[i + 1]))
                }
                _routeSegments.value = straightSegments
                _routeLoadStatus.value = RouteLoadStatus(0, plans.size - 1, plans.size - 1)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun parseGeoJSONToPoints(geometryJson: JsonElement): List<GeoPoint> {
        return try {
            val geometryObj = geometryJson.asJsonObject
            val coordinates = geometryObj.getAsJsonArray("coordinates")

            val geoPoints = mutableListOf<GeoPoint>()
            coordinates.forEach { coord ->
                val point = coord.asJsonArray
                val lon = point[0].asDouble
                val lat = point[1].asDouble
                geoPoints.add(GeoPoint(lat, lon))
            }
            geoPoints
        } catch (e: Exception) {
            Log.e("TripMapViewModel", "Error parsing GeoJSON", e)
            emptyList()
        }
    }

    private fun createStraightSegment(fromPlan: PlanLocation, toPlan: PlanLocation): List<GeoPoint> {
        return listOf(
            GeoPoint(fromPlan.latitude, fromPlan.longitude),
            GeoPoint(toPlan.latitude, toPlan.longitude)
        )
    }
}

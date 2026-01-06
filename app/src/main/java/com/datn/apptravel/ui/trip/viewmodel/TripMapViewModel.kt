package com.datn.apptravel.ui.trip.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.BuildConfig
import com.datn.apptravel.R
import com.datn.apptravel.data.api.OSRMRetrofitClient
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.model.User
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.ui.base.BaseViewModel
import com.datn.apptravel.ui.trip.model.PlanLocation
import com.datn.apptravel.ui.trip.model.ScheduleItem
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TripMapViewModel(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    private val _planLocations = MutableLiveData<List<PlanLocation>>()
    val planLocations: LiveData<List<PlanLocation>> = _planLocations

    private val _tripDates = MutableLiveData<Pair<String, String>>()
    val tripDates: LiveData<Pair<String, String>> = _tripDates
    
    private val _scheduleItems = MutableLiveData<List<ScheduleItem>>()
    val scheduleItems: LiveData<List<ScheduleItem>> = _scheduleItems

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

                // Load trip to get start/end dates and members
                var tripMembers: List<User>? = null
                tripRepository.getTripById(tripId).onSuccess { trip ->
                    _tripDates.value = Pair(trip.startDate, trip.endDate)
                    tripMembers = trip.members
                }

                // Load plans from API
                tripRepository.getPlansByTripId(tripId).onSuccess { apiPlans ->
                    Log.d("TripMapViewModel", "=== PLAN LOADING DEBUG ===")
                    Log.d("TripMapViewModel", "Total plans from API: ${apiPlans.size}")
                    
                    if (apiPlans.isEmpty()) {
//                        setError("No plans found for this trip")
                        _planLocations.value = emptyList()
                        setLoading(false)
                        return@onSuccess
                    }

                    // Check if current user is owner or member
                    val isOwner = currentUserId != null && tripUserId != null && currentUserId == tripUserId
                    val isMember = currentUserId != null && tripMembers?.any { it.id == currentUserId } == true
                    
                    Log.d("TripMapViewModel", "User permissions - isOwner: $isOwner, isMember: $isMember")
                    
                    // Filter plans based on permissions
                    val filteredPlans = if (!isOwner && !isMember) {
                        // User is neither owner nor member - only show plans that have already occurred
                        // Plan của hôm nay chỉ hiển thị vào ngày mai (planDate < today)
                        val today = java.time.LocalDate.now()
                        val filtered = apiPlans.filter { plan ->
                            try {
                                val planDateTime = LocalDateTime.parse(
                                    plan.startTime,
                                    DateTimeFormatter.ISO_DATE_TIME
                                )
                                val planDate = planDateTime.toLocalDate()
                                // Show only plans before today (planDate < today)
                                // Plans của hôm nay (planDate == today) sẽ hiển thị vào ngày mai
                                planDate.isBefore(today)
                            } catch (e: Exception) {
                                Log.e("TripMapViewModel", "Error parsing date for plan: ${plan.title}", e)
                                false // Don't show plans with invalid dates
                            }
                        }
                        Log.d("TripMapViewModel", "After date filtering (non-owner): ${filtered.size} plans")
                        filtered
                    } else {
                        // User is owner or member - show all plans
                        Log.d("TripMapViewModel", "Owner/member mode - showing all ${apiPlans.size} plans")
                        apiPlans
                    }

                    // Convert API plans to PlanLocation with geocoding
                    val planLocations = convertPlansToLocations(filteredPlans, packageName)
                    Log.d("TripMapViewModel", "After coordinate conversion: ${planLocations.size} valid locations")
                    Log.d("TripMapViewModel", "Plans lost in conversion: ${filteredPlans.size - planLocations.size}")
                    _planLocations.value = planLocations

                    if (planLocations.isNotEmpty()) {
                        _centerLocation.value = GeoPoint(
                            planLocations[0].latitude,
                            planLocations[0].longitude
                        )
                    }
                    
                    // Update schedule items after plans are loaded
                    updateScheduleItems()
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
            var successCount = 0
            var failedCount = 0
            
            val results = apiPlans.mapNotNull { plan ->
                try {
                    // Parse time from ISO format
                    val time = try {
                        val dateTime = LocalDateTime.parse(
                            plan.startTime,
                            DateTimeFormatter.ISO_DATE_TIME
                        )
                        String.format("%02d:%02d", dateTime.hour, dateTime.minute)
                    } catch (e: Exception) {
                        Log.w("TripMapViewModel", "Time parse error for ${plan.title}: ${e.message}")
                        "00:00"
                    }

                    // Get coordinates from location field (format: "latitude,longitude")
                    val locationStr = plan.location
                    Log.d("TripMapViewModel", "Plan: ${plan.title}, location field: '$locationStr', address: '${plan.address}'")
                    
                    val coordinates = if (!locationStr.isNullOrBlank()) {
                        try {
                            val parts = locationStr.split(",")
                            if (parts.size == 2) {
                                val lat = parts[0].trim().toDouble()
                                val lon = parts[1].trim().toDouble()
                                Log.d("TripMapViewModel", "✓ Parsed coordinates from location field: ($lat, $lon)")
                                Pair(lat, lon)
                            } else {
                                Log.w("TripMapViewModel", "✗ Invalid location format (not 2 parts): $locationStr")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("TripMapViewModel", "✗ Error parsing location '$locationStr': ${e.message}")
                            null
                        }
                    } else {
                        // Fallback: geocode from address if location is not available
                        Log.d("TripMapViewModel", "Location field empty, trying geocode for: ${plan.address ?: plan.title}")
                        val geocoded = geocodeLocation(plan.address ?: plan.title, packageName)
                        if (geocoded != null) {
                            Log.d("TripMapViewModel", "✓ Geocoded: ${geocoded.first}, ${geocoded.second}")
                        } else {
                            Log.w("TripMapViewModel", "✗ Geocoding failed for: ${plan.address ?: plan.title}")
                        }
                        geocoded
                    }

                    if (coordinates != null) {
                        successCount++
                        PlanLocation(
                            planId = plan.id ?: run {
                                Log.e("TripMapViewModel", "✗ Plan has no ID: ${plan.title}")
                                failedCount++
                                return@mapNotNull null
                            },
                            name = plan.title,
                            time = time,
                            detail = plan.address ?: "",
                            latitude = coordinates.first,
                            longitude = coordinates.second,
                            iconResId = getIconForPlanType(plan.type),
                            photoUrl = plan.photoUrl
                        )
                    } else {
                        failedCount++
                        Log.w("TripMapViewModel", "✗ Could not get coordinates for: ${plan.title}")
                        null
                    }
                } catch (e: Exception) {
                    failedCount++
                    Log.e("TripMapViewModel", "✗ Error converting plan: ${plan.title}", e)
                    null
                }
            }
            
            Log.d("TripMapViewModel", "Conversion summary: $successCount success, $failedCount failed out of ${apiPlans.size} plans")
            results
        }
    }

    private suspend fun geocodeLocation(
        address: String,
        packageName: String
    ): Pair<Double, Double>? {
        return try {
            // Use Nominatim (OpenStreetMap) geocoding API
            val response = withContext(Dispatchers.IO) {
                val baseUrl = BuildConfig.NOMINATIM_BASE_URL
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
    
    private fun updateScheduleItems() {
        val dates = _tripDates.value ?: return
        val plans = _planLocations.value ?: emptyList()
        val startDate = dates.first
        val endDate = dates.second
        
        if (startDate.isEmpty() || endDate.isEmpty()) return

        val items = mutableListOf<ScheduleItem>()

        // Add Start date
        items.add(
            ScheduleItem.DateItem(
                label = "Start",
                date = formatDate(startDate)
            )
        )

        // Add all plans with connectors between them
        plans.forEachIndexed { index, plan ->
            // Add connector before this plan (except for the first plan)
            if (index > 0) {
                items.add(
                    ScheduleItem.ConnectorItem(
                        fromPlanPosition = index - 1,
                        toPlanPosition = index
                    )
                )
            }

            items.add(
                ScheduleItem.PlanItem(
                    plan = plan,
                    position = index
                )
            )
        }

        // Add End date
        items.add(
            ScheduleItem.DateItem(
                label = "End",
                date = formatDate(endDate)
            )
        )

        _scheduleItems.value = items
    }
    
    private fun formatDate(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }
}

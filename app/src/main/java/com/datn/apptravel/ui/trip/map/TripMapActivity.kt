package com.datn.apptravel.ui.trip.map

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.data.api.OSRMRetrofitClient
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.repository.TripRepository
import com.datn.apptravel.databinding.ActivityTripMapBinding
import com.datn.apptravel.ui.trip.adapter.ScheduleAdapter
import com.datn.apptravel.ui.trip.model.PlanLocation
import com.datn.apptravel.ui.trip.model.ScheduleItem
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TripMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripMapBinding
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val plans = mutableListOf<PlanLocation>()
    private val scheduleItems = mutableListOf<ScheduleItem>()
    private val routePolylines = mutableListOf<Polyline>()
    private val routeSegments = mutableListOf<List<GeoPoint>>() // Store each segment's points
    private val markers = mutableListOf<Marker>()
    private var highlightedPolyline: Polyline? = null

    private val tripRepository: TripRepository by inject()
    private val setZoom = 14.0
    private var tripId: String? = null
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityTripMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupMap()
        loadSampleData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup RecyclerView with HORIZONTAL layout and scroll listener
        scheduleAdapter = ScheduleAdapter(
            items = scheduleItems,
            onPlanClick = { position, plan ->
                onPlanClicked(position, plan)
            },
            onConnectorClick = { fromPos, toPos ->
                onConnectorClicked(fromPos, toPos)
            }
        )

        binding.rvPlans.apply {
            layoutManager =
                LinearLayoutManager(this@TripMapActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = scheduleAdapter

            // Detect scroll to highlight plans
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    highlightVisiblePlan()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    // When scroll stops, highlight the most visible item
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        highlightVisiblePlan()
                    }
                }
            })
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            // Enable rotation
            val rotationGestureOverlay = RotationGestureOverlay(this)
            rotationGestureOverlay.isEnabled = true
            overlays.add(rotationGestureOverlay)

            controller.setZoom(setZoom)
        }
    }

    private fun loadSampleData() {
        // Get trip info from intent
        tripId = intent.getStringExtra("tripId")
        val tripTitle = intent.getStringExtra("tripTitle") ?: "Trip"

        if (tripId == null) {
            Toast.makeText(this, "Trip ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE

        // Load trip details and plans
        lifecycleScope.launch {
            // Load trip to get start/end dates
            tripRepository.getTripById(tripId!!).onSuccess { trip ->
                startDate = trip.startDate
                endDate = trip.endDate
            }

            // Load real plans from API
            tripRepository.getPlansByTripId(tripId!!).onSuccess { apiPlans ->
                // Convert API plans to PlanLocation with geocoding
                val planLocations = convertPlansToLocations(apiPlans)

                withContext(Dispatchers.Main) {
                    plans.clear()
                    plans.addAll(planLocations)

                    // Build schedule items: Start + Plans + End
                    scheduleItems.clear()

                    // Add Start date
                    scheduleItems.add(
                        ScheduleItem.DateItem(
                            label = "Start",
                            date = formatDate(startDate)
                        )
                    )

                    // Add all plans with connectors between them
                    planLocations.forEachIndexed { index, plan ->
                        // Add connector before this plan (except for the first plan)
                        if (index > 0) {
                            scheduleItems.add(
                                ScheduleItem.ConnectorItem(
                                    fromPlanPosition = index - 1,
                                    toPlanPosition = index
                                )
                            )
                        }

                        scheduleItems.add(
                            ScheduleItem.PlanItem(
                                plan = plan,
                                position = index
                            )
                        )
                    }

                    // Add End date
                    scheduleItems.add(
                        ScheduleItem.DateItem(
                            label = "End",
                            date = formatDate(endDate)
                        )
                    )

                    scheduleAdapter.notifyDataSetChanged()

                    if (plans.isEmpty()) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@TripMapActivity,
                            "No plans found for this trip",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@withContext
                    }

                    // Add markers to map
                    addMarkersToMap()

                    // Draw route
                    drawRoute()

                    // Center map
                    val centerPoint = GeoPoint(plans[0].latitude, plans[0].longitude)
                    binding.mapView.controller.setCenter(centerPoint)

                    binding.progressBar.visibility = View.GONE
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@TripMapActivity,
                        "Failed to load plans: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("TripMapActivity", "Error loading plans", error)
                }
            }
        }
    }


    private fun formatDate(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }

    private suspend fun convertPlansToLocations(apiPlans: List<Plan>): List<PlanLocation> {
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
                            Log.e("TripMapActivity", "Error parsing location: $locationStr", e)
                            null
                        }
                    } else {
                        // Fallback: geocode from address if location is not available
                        geocodeLocation(plan.address ?: plan.title)
                    }

                    if (coordinates != null) {
                        PlanLocation(
                            name = plan.title,
                            time = time,
                            detail = plan.address ?: "",
                            latitude = coordinates.first,
                            longitude = coordinates.second,
                            iconResId = getIconForPlanType(plan.type)
                        )
                    } else {
                        Log.w("TripMapActivity", "Could not get coordinates for: ${plan.title}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("TripMapActivity", "Error converting plan: ${plan.title}", e)
                    null
                }
            }
        }
    }

    private suspend fun geocodeLocation(address: String): Pair<Double, Double>? {
        return try {
            // Use Nominatim (OpenStreetMap) geocoding API
            val response = withContext(Dispatchers.IO) {
                val url = "https://nominatim.openstreetmap.org/search?q=${
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
            val jsonArray = Gson().fromJson(
                response,
                JsonArray::class.java
            )

            if (jsonArray.size() > 0) {
                val firstResult = jsonArray[0].asJsonObject
                val lat = firstResult.get("lat").asDouble
                val lon = firstResult.get("lon").asDouble
                Pair(lat, lon)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TripMapActivity", "Geocoding error for: $address", e)
            null
        }
    }

    private fun getIconForPlanType(planType: PlanType): Int {
        return when (planType) {
            PlanType.RESTAURANT -> R.drawable.ic_restaurant
            PlanType.LODGING -> R.drawable.ic_lodging
            PlanType.FLIGHT -> R.drawable.ic_flight
            PlanType.BOAT -> R.drawable.ic_boat
            PlanType.CAR_RENTAL -> R.drawable.ic_car
            PlanType.ACTIVITY -> R.drawable.ic_attraction
            else -> R.drawable.ic_location
        }
    }

    private fun addMarkersToMap() {
        markers.clear()
        binding.mapView.overlays.removeAll { it is Marker }

        plans.forEachIndexed { index, plan ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(plan.latitude, plan.longitude)
                title = plan.name
                snippet = "${plan.time} - ${plan.detail}"

                // Customize marker icon based on plan type
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { marker, _ ->
                    onPlanClicked(index, plan)
                    true
                }
            }

            markers.add(marker)
            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun drawRoute() {
        if (plans.size < 2) return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Clear existing polylines and segments
            routePolylines.forEach { binding.mapView.overlays.remove(it) }
            routePolylines.clear()
            routeSegments.clear()

            try {
                // Draw each segment between consecutive plans separately
                for (i in 0 until plans.size - 1) {
                    val fromPlan = plans[i]
                    val toPlan = plans[i + 1]

                    // Build coordinates string for this segment only
                    val coordinates = "${fromPlan.longitude},${fromPlan.latitude};${toPlan.longitude},${toPlan.latitude}"

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
                        route?.let {
                            // Parse and draw this segment
                            drawSegmentFromGeoJSON(it.geometry, i)
                        }
                    } else {
                        // Fallback: draw straight line for this segment
                        drawStraightSegment(i)
                    }
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TripMapActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Fallback: draw all straight lines
                drawStraightLines()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun drawRouteOnMap(encodedPolyline: String) {
        // This function is now deprecated, use drawSegmentFromGeoJSON instead
        drawStraightLines()
    }

    private fun drawSegmentFromGeoJSON(geometryJson: JsonElement, segmentIndex: Int) {
        try {
            // Parse GeoJSON geometry
            val geometryObj = geometryJson.asJsonObject
            val coordinates = geometryObj.getAsJsonArray("coordinates")

            val geoPoints = mutableListOf<GeoPoint>()
            coordinates.forEach { coord ->
                val point = coord.asJsonArray
                val lon = point[0].asDouble
                val lat = point[1].asDouble
                geoPoints.add(GeoPoint(lat, lon))
            }

            if (geoPoints.isEmpty()) {
                drawStraightSegment(segmentIndex)
                return
            }

            // Store segment points
            routeSegments.add(geoPoints)

            // Draw this segment
            val polyline = Polyline().apply {
                setPoints(geoPoints)
                outlinePaint.color = Color.parseColor("#DC2626") // Red default
                outlinePaint.strokeWidth = 10f
                outlinePaint.isAntiAlias = true
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }

            routePolylines.add(polyline)
            binding.mapView.overlays.add(0, polyline)
            binding.mapView.invalidate()

        } catch (e: Exception) {
            e.printStackTrace()
            drawStraightSegment(segmentIndex)
        }
    }

    private fun drawStraightSegment(segmentIndex: Int) {
        if (segmentIndex >= plans.size - 1) return

        val fromPlan = plans[segmentIndex]
        val toPlan = plans[segmentIndex + 1]

        val segmentPoints = listOf(
            GeoPoint(fromPlan.latitude, fromPlan.longitude),
            GeoPoint(toPlan.latitude, toPlan.longitude)
        )

        routeSegments.add(segmentPoints)

        val polyline = Polyline().apply {
            setPoints(segmentPoints)
            outlinePaint.color = Color.parseColor("#DC2626") // Red default
            outlinePaint.strokeWidth = 10f
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }

        routePolylines.add(polyline)
        binding.mapView.overlays.add(0, polyline)
        binding.mapView.invalidate()
    }

    private fun drawRouteFromGeoJSON(geometryJson: JsonElement) {
        // This function is deprecated - now using drawSegmentFromGeoJSON for each segment
        drawStraightLines()
    }

    private fun adjustMapBounds(points: List<GeoPoint>) {
        if (points.isEmpty()) return

        val boundingBox = BoundingBox.fromGeoPoints(points)
        binding.mapView.post {
            binding.mapView.zoomToBoundingBox(boundingBox, true, 100)
        }
    }

    private fun drawStraightLines() {
        routePolylines.forEach { binding.mapView.overlays.remove(it) }
        routePolylines.clear()
        routeSegments.clear()

        // Draw separate lines between each pair of consecutive plans
        for (i in 0 until plans.size - 1) {
            drawStraightSegment(i)
        }

        // Adjust map bounds
        val allPoints = plans.map { GeoPoint(it.latitude, it.longitude) }
        adjustMapBounds(allPoints)
    }

    private fun onPlanClicked(position: Int, plan: PlanLocation) {
        // Highlight plan in list
        scheduleAdapter.highlightPlan(position)

        // Calculate adapter position: Start date (1) + plan position + connectors before it
        val adapterPosition = 1 + position + position
        binding.rvPlans.smoothScrollToPosition(adapterPosition)

        // Center map on plan
        val geoPoint = GeoPoint(plan.latitude, plan.longitude)
        binding.mapView.controller.animateTo(geoPoint)

        // Highlight marker
        highlightMarker(position)

        // Highlight route segments
        highlightRouteSegment(position)
    }

    private fun onConnectorClicked(fromPos: Int, toPos: Int) {
        // Calculate adapter position for this connector
        val connectorAdapterPos = 1 + toPos + (toPos - 1)
        scheduleAdapter.highlightConnector(connectorAdapterPos)

        // Scroll to connector
        binding.rvPlans.smoothScrollToPosition(connectorAdapterPos)

        // Highlight route segment between two plans on map
        highlightRouteSegmentBetween(fromPos, toPos)

        // Center map on midpoint between the two plans
        if (fromPos in plans.indices && toPos in plans.indices) {
            val fromPlan = plans[fromPos]
            val toPlan = plans[toPos]
            val midLat = (fromPlan.latitude + toPlan.latitude) / 2
            val midLon = (fromPlan.longitude + toPlan.longitude) / 2
            binding.mapView.controller.animateTo(GeoPoint(midLat, midLon))
        }
    }

    private fun highlightVisiblePlan() {
        val layoutManager = binding.rvPlans.layoutManager as? LinearLayoutManager ?: return

        // For horizontal scroll, find the item closest to center
        val recyclerViewCenter = binding.rvPlans.width / 2
        var closestPosition = -1
        var closestDistance = Int.MAX_VALUE

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        var closestItem: ScheduleItem? = null
        var closestItemAdapterPos = -1

        for (i in firstVisible..lastVisible) {
            // Skip date items (first and last)
            if (i == 0 || i == scheduleItems.size - 1) continue

            val item = scheduleItems.getOrNull(i) ?: continue

            val view = layoutManager.findViewByPosition(i) ?: continue
            val viewCenter = (view.left + view.right) / 2
            val distance = Math.abs(recyclerViewCenter - viewCenter)

            if (distance < closestDistance) {
                closestDistance = distance
                closestItem = item
                closestItemAdapterPos = i
                if (item is ScheduleItem.PlanItem) {
                    closestPosition = item.position
                }
            }
        }

        // Handle connector items
        if (closestItem is ScheduleItem.ConnectorItem) {
            scheduleAdapter.highlightConnector(closestItemAdapterPos)
            highlightRouteSegmentBetween(closestItem.fromPlanPosition, closestItem.toPlanPosition)

            // Center map on midpoint
            if (closestItem.fromPlanPosition in plans.indices && closestItem.toPlanPosition in plans.indices) {
                val fromPlan = plans[closestItem.fromPlanPosition]
                val toPlan = plans[closestItem.toPlanPosition]
                val midLat = (fromPlan.latitude + toPlan.latitude) / 2
                val midLon = (fromPlan.longitude + toPlan.longitude) / 2
                binding.mapView.controller.animateTo(GeoPoint(midLat, midLon))
            }
        }
        // Handle plan items
        else if (closestPosition >= 0 && closestPosition < plans.size) {
            scheduleAdapter.highlightPlan(closestPosition)
            highlightMarker(closestPosition)
            highlightRouteSegment(closestPosition)

            // Optional: Center map on the highlighted plan
            val plan = plans[closestPosition]
            val geoPoint = GeoPoint(plan.latitude, plan.longitude)
            binding.mapView.controller.animateTo(geoPoint)
        }
    }

    private fun highlightMarker(position: Int) {
        // Reset all markers to normal
        markers.forEach { marker ->
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        // Highlight selected marker (make it larger or change icon)
        if (position in markers.indices) {
            val marker = markers[position]
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            // You can change icon or size here
        }

        binding.mapView.invalidate()
    }

    private fun highlightRouteSegment(planPosition: Int) {
        // Reset all segments to red (no special highlight for individual plans)
        routePolylines.forEach { polyline ->
            polyline.outlinePaint.color = Color.parseColor("#DC2626") // Red
            polyline.outlinePaint.strokeWidth = 10f
        }

        highlightedPolyline = null
        binding.mapView.invalidate()
    }

    private fun highlightRouteSegmentBetween(fromPos: Int, toPos: Int) {
        // Reset all segments to red
        routePolylines.forEach { polyline ->
            polyline.outlinePaint.color = Color.parseColor("#DC2626") // Red
            polyline.outlinePaint.strokeWidth = 10f
        }

        // Highlight the specific segment in yellow
        if (fromPos in plans.indices && toPos in plans.indices) {
            // The segment index is fromPos (segment from plan[fromPos] to plan[toPos])
            val segmentIndex = fromPos
            if (segmentIndex in routePolylines.indices) {
                val polyline = routePolylines[segmentIndex]
                polyline.outlinePaint.color = Color.parseColor("#FFC107") // Yellow highlight
                polyline.outlinePaint.strokeWidth = 14f
                highlightedPolyline = polyline
            }
        }

        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
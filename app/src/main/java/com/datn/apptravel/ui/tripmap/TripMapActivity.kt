package com.datn.apptravel.ui.tripmap

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.data.api.OSRMRetrofitClient
import com.datn.apptravel.databinding.ActivityTripMapBinding
import com.datn.apptravel.ui.model.PlanLocation
import com.datn.apptravel.ui.adapter.PlanMapAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

class TripMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripMapBinding
    private lateinit var planAdapter: PlanMapAdapter
    private val plans = mutableListOf<PlanLocation>()
    private val routePolylines = mutableListOf<Polyline>()
    private val markers = mutableListOf<Marker>()
    private var highlightedPolyline: Polyline? = null

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
        planAdapter = PlanMapAdapter(plans) { position, plan ->
            onPlanClicked(position, plan)
        }

        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(this@TripMapActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = planAdapter
            
            // Add snap helper to center items when scrolling
            val snapHelper = androidx.recyclerview.widget.LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)
            
            // Detect scroll to highlight plans
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    highlightVisiblePlan()
                }
                
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    // When scroll stops, highlight the centered item
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
            
            controller.setZoom(13.0)
        }
    }

    private fun loadSampleData() {
        // Get trip info from intent
        val tripId = intent.getStringExtra("tripId")
        val tripTitle = intent.getStringExtra("tripTitle") ?: "Sample Trip"
        
        // Load plans with real coordinates
        plans.clear()
        plans.addAll(
            com.datn.apptravel.util.TripPlanManager.getPlansByTripId(tripId ?: "", tripTitle)
        )

        planAdapter.notifyDataSetChanged()
        
        // Add markers to map
        addMarkersToMap()
        
        // Draw route
        drawRoute()
        
        // Center map
        if (plans.isNotEmpty()) {
            val centerPoint = GeoPoint(plans[0].latitude, plans[0].longitude)
            binding.mapView.controller.setCenter(centerPoint)
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
            try {
                // Build coordinates string for OSRM: "lon1,lat1;lon2,lat2;..."
                val coordinates = plans.joinToString(";") { plan ->
                    "${plan.longitude},${plan.latitude}"
                }

                val response = withContext(Dispatchers.IO) {
                    OSRMRetrofitClient.apiService.getRoute(
                        coordinates = coordinates,
                        overview = "full",
                        geometries = "geojson",  // Changed to geojson for easier parsing
                        steps = true
                    )
                }

                if (response.isSuccessful && response.body()?.code == "Ok") {
                    val route = response.body()?.routes?.firstOrNull()
                    route?.let {
                        // OSRM returns geometry in GeoJSON format
                        drawRouteFromGeoJSON(it.geometry)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TripMapActivity,
                            "Route API failed, showing straight lines",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // Fallback: draw straight lines between points
                    drawStraightLines()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TripMapActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Fallback: draw straight lines
                drawStraightLines()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun drawRouteOnMap(encodedPolyline: String) {
        // This function is now deprecated, use drawRouteFromGeoJSON instead
        drawStraightLines()
    }

    private fun drawRouteFromGeoJSON(geometryJson: com.google.gson.JsonElement) {
        // Clear existing polylines
        routePolylines.forEach { binding.mapView.overlays.remove(it) }
        routePolylines.clear()

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
                drawStraightLines()
                return
            }

            // Draw the entire route as one polyline
            val polyline = Polyline().apply {
                setPoints(geoPoints)
                outlinePaint.color = Color.parseColor("#2563EB")
                outlinePaint.strokeWidth = 10f
                outlinePaint.isAntiAlias = true
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
            }
            
            routePolylines.add(polyline)
            binding.mapView.overlays.add(0, polyline) // Add at index 0 so markers are on top

            binding.mapView.invalidate()
            
            // Adjust map bounds to show entire route
            adjustMapBounds(geoPoints)
        } catch (e: Exception) {
            e.printStackTrace()
            drawStraightLines()
        }
    }

    private fun adjustMapBounds(points: List<GeoPoint>) {
        if (points.isEmpty()) return
        
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
        binding.mapView.post {
            binding.mapView.zoomToBoundingBox(boundingBox, true, 100)
        }
    }

    private fun drawStraightLines() {
        routePolylines.forEach { binding.mapView.overlays.remove(it) }
        routePolylines.clear()

        // Draw as one continuous line through all points
        val allPoints = plans.map { GeoPoint(it.latitude, it.longitude) }
        
        val polyline = Polyline().apply {
            setPoints(allPoints)
            outlinePaint.color = Color.parseColor("#2563EB")
            outlinePaint.strokeWidth = 10f
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
        }

        routePolylines.add(polyline)
        binding.mapView.overlays.add(0, polyline)

        binding.mapView.invalidate()
        
        // Adjust map bounds
        adjustMapBounds(allPoints)
    }

    private fun onPlanClicked(position: Int, plan: PlanLocation) {
        // Highlight plan in list
        planAdapter.highlightPlan(position)
        
        // Scroll to plan if not visible
        binding.rvPlans.smoothScrollToPosition(position)
        
        // Center map on plan
        val geoPoint = GeoPoint(plan.latitude, plan.longitude)
        binding.mapView.controller.animateTo(geoPoint)
        
        // Highlight marker
        highlightMarker(position)
        
        // Highlight route segments
        highlightRouteSegment(position)
    }

    private fun highlightVisiblePlan() {
        val layoutManager = binding.rvPlans.layoutManager as? LinearLayoutManager ?: return
        
        // For horizontal scroll, find the item closest to center
        val recyclerViewCenter = binding.rvPlans.width / 2
        var closestPosition = -1
        var closestDistance = Int.MAX_VALUE
        
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        for (i in firstVisible..lastVisible) {
            val view = layoutManager.findViewByPosition(i) ?: continue
            val viewCenter = (view.left + view.right) / 2
            val distance = Math.abs(recyclerViewCenter - viewCenter)
            
            if (distance < closestDistance) {
                closestDistance = distance
                closestPosition = i
            }
        }
        
        if (closestPosition >= 0 && closestPosition < plans.size) {
            planAdapter.highlightPlan(closestPosition)
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
        // Clear previous highlight
        highlightedPolyline?.let {
            it.outlinePaint.color = Color.parseColor("#2563EB")
            it.outlinePaint.strokeWidth = 10f
        }

        // With full route, we highlight the whole route when any plan is selected
        if (routePolylines.isNotEmpty()) {
            val polyline = routePolylines[0]
            polyline.outlinePaint.color = Color.parseColor("#DC2626") // Red highlight
            polyline.outlinePaint.strokeWidth = 14f
            highlightedPolyline = polyline
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

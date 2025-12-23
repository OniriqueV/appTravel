package com.datn.apptravel.ui.trip.map

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.databinding.ActivityTripMapBinding
import com.datn.apptravel.ui.trip.adapter.ScheduleTripMapAdapter
import com.datn.apptravel.ui.trip.model.PlanLocation
import com.datn.apptravel.ui.trip.model.ScheduleItem
import com.datn.apptravel.ui.trip.viewmodel.TripMapViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.android.ext.android.inject


class TripMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripMapBinding
    private val viewModel: TripMapViewModel by viewModel()
    private val authRepository: AuthRepository by inject()
    private lateinit var scheduleAdapter: ScheduleTripMapAdapter
    private val plans = mutableListOf<PlanLocation>()
    private val scheduleItems = mutableListOf<ScheduleItem>()
    private val routePolylines = mutableListOf<Polyline>()
    private val markers = mutableListOf<Marker>()
    private var highlightedPolyline: Polyline? = null

    private val setZoom = 14.0
    private var tripId: String? = null
    private var tripUserId: String? = null
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
        observeViewModel()
        loadTripData()
    }

    private fun observeViewModel() {
        // Observe plan locations
        viewModel.planLocations.observe(this) { planLocations ->
            plans.clear()
            plans.addAll(planLocations)

            // Always update schedule items to show start/end dates even without plans
            updateScheduleItems()
            
            if (plans.isNotEmpty()) {
                addMarkersToMap()
                viewModel.drawRoute(plans)
            } else {
                // No plans, but still show the map with trip location (if available)
//                Toast.makeText(this, "No plans found for this trip", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe trip dates
        viewModel.tripDates.observe(this) { (start, end) ->
            startDate = start
            endDate = end
            updateScheduleItems()
        }

        // Observe route segments
        viewModel.routeSegments.observe(this) { segments ->
            drawRouteSegments(segments)
        }

        // Observe center location
        viewModel.centerLocation.observe(this) { location ->
            binding.mapView.controller.setCenter(location)
        }

        // Observe route load status
        viewModel.routeLoadStatus.observe(this) { status ->
            val message = when {
                status.successful == status.total -> "Route loaded successfully"
                status.successful > 0 -> "Route partially loaded (${status.successful}/${status.total} segments)"
                else -> "Unable to load routes. Showing direct paths."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            showLoadingOverlay(isLoading)
        }

        // Observe errors
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup RecyclerView with HORIZONTAL layout and scroll listener
        scheduleAdapter = ScheduleTripMapAdapter(
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
            
            // Set default center position (Vietnam center) in case no plans available
            controller.setCenter(GeoPoint(16.0544, 108.2022))
        }
    }

    private fun loadTripData() {
        // Get trip info from intent
        tripId = intent.getStringExtra("tripId")
        tripUserId = intent.getStringExtra("tripUserId")
        val tripTitle = intent.getStringExtra("tripTitle") ?: "Trip"

        if (tripId == null) {
            Toast.makeText(this, "Trip ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user ID and load data through ViewModel
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    val currentUserId = user.id
                    viewModel.loadTripData(tripId!!, packageName, currentUserId, tripUserId)
                }
                return@collect
            }
        }
    }

    private fun updateScheduleItems() {
        if (startDate.isNullOrEmpty() || endDate.isNullOrEmpty()) return


        scheduleItems.clear()

        // Add Start date
        scheduleItems.add(
            ScheduleItem.DateItem(
                label = "Start",
                date = formatDate(startDate)
            )
        )

        // Add all plans with connectors between them
        plans.forEachIndexed { index, plan ->
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
    }

    private fun formatDate(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            dateString
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

    private fun drawRouteSegments(segments: List<List<GeoPoint>>) {
        // Clear existing polylines
        routePolylines.forEach { binding.mapView.overlays.remove(it) }
        routePolylines.clear()

        // Draw each segment
        segments.forEach { segmentPoints ->
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
        }

        binding.mapView.invalidate()

        // Adjust map bounds to show all points
        val allPoints = segments.flatten()
        if (allPoints.isNotEmpty()) {
            adjustMapBounds(allPoints)
        }
    }

    private fun adjustMapBounds(points: List<GeoPoint>) {
        if (points.isEmpty()) return

        val boundingBox = BoundingBox.fromGeoPoints(points)
        binding.mapView.post {
            binding.mapView.zoomToBoundingBox(boundingBox, true, 100)
        }
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
        var closestDistance = Int.MAX_VALUE

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        var closestItem: ScheduleItem? = null
        var closestItemAdapterPos = -1
        var closestPosition = -1

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

        // Highlight the specific segment in blue
        if (fromPos in plans.indices && toPos in plans.indices) {
            // The segment index is fromPos (segment from plan[fromPos] to plan[toPos])
            val segmentIndex = fromPos
            if (segmentIndex in routePolylines.indices) {
                val polyline = routePolylines[segmentIndex]
                polyline.outlinePaint.color = Color.parseColor("#2196F3") // blue highlight
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

    private fun showLoadingOverlay(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE

        // Disable/enable interactions when loading
        binding.btnBack.isEnabled = !show
        binding.mapView.isEnabled = !show
        binding.rvPlans.isEnabled = !show
    }
}
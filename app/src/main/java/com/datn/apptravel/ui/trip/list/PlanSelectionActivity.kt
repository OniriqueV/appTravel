package com.datn.apptravel.ui.trip.list

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.data.model.response.MapPlace
import com.datn.apptravel.data.repository.ImageSearchRepository
import com.datn.apptravel.databinding.ActivityPlanSelectionBinding
import com.datn.apptravel.ui.trip.detail.plandetail.ActivityDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.BoatDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.CarRentalDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.FlightDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.LodgingDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.PlanSelectionViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class PlanSelectionActivity : AppCompatActivity() {

    private val viewModel: PlanSelectionViewModel by viewModel()
    private val imageSearchRepository: ImageSearchRepository by inject()
    private var tripId: String? = null
    private lateinit var binding: ActivityPlanSelectionBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationOverlay: MyLocationNewOverlay? = null

    private var currentLatitude = 21.0285
    private var currentLongitude = 105.8542
    
    // Store the photo URL to pass to detail activities
    private var selectedPhotoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        binding = ActivityPlanSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get trip ID from intent
        tripId = intent.getStringExtra("tripId")

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupUI()
        setupObservers()
        checkLocationPermission()
    }


    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)

            // Set default position (Hanoi)
            controller.setCenter(GeoPoint(currentLatitude, currentLongitude))
        }

        // Add location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        myLocationOverlay?.enableMyLocation()
        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun setupUI() {
        // Set up back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        // Set up semi-circle menu
        binding.semiCircleMenu.setOnPlanSelectedListener { planType ->
            // Show loading overlay and disable interactions
            showLoadingOverlay(true)
            
            // Check if there's text in search view (even if user didn't press Enter)
            val searchQuery = binding.searchView.query?.toString()
            android.util.Log.d("PlanSelection", "Plan type selected: ${planType.displayName}, Current search query: '$searchQuery'")
            
            if (!searchQuery.isNullOrBlank()) {
                // User has typed something - search for that location first, then show plan type
                android.util.Log.d("PlanSelection", "Searching for location: $searchQuery before showing plan type")
                viewModel.searchPlacesWithPlanType(searchQuery, planType, currentLatitude, currentLongitude)
            } else {
                // No search query - just show plan type at current location
                android.util.Log.d("PlanSelection", "No search query, showing plan type at current location")
                viewModel.selectPlanType(planType, currentLatitude, currentLongitude)
            }
        }

        // Set up search view
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                android.util.Log.d("PlanSelection", "onQueryTextSubmit called with: $query")
                
                query?.let {
                    if (it.isNotBlank()) {
                        // Search for the location and save its coordinates
                        val timestamp = System.currentTimeMillis()
                        android.util.Log.d("PlanSelection", "[$timestamp] Searching for location: $it")
                        
                        // If plan type is selected, search places at that location
                        // If not, just find and save the location coordinates
                        viewModel.searchPlaces(it, currentLatitude, currentLongitude)

                        // Hide keyboard
                        binding.searchView.clearFocus()
                    } else {
                        android.util.Log.d("PlanSelection", "Empty query submitted, clearing search")
                        // If empty query submitted, clear search
                        viewModel.clearSearch(currentLatitude, currentLongitude)
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                android.util.Log.d("PlanSelection", "onQueryTextChange: $newText")
                // Do nothing - only search on Enter/Submit
                return true
            }
        })
    }

    /**
     * Setup observers for ViewModel LiveData
     */
    private fun setupObservers() {
        viewModel.places.observe(this) { places ->
            // Clear existing markers (keep location overlay)
            binding.mapView.overlays.removeAll { it is Marker }

            // Add markers for each place
            places.forEach { place ->
                val marker = Marker(binding.mapView)
                marker.position = GeoPoint(place.latitude, place.longitude)
                marker.title = place.name
                marker.snippet = place.address
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Set custom marker icon
                val icon = resources.getDrawable(R.drawable.ic_marker, null)
                icon.setBounds(0, 0, 20, 20) // Set size to 80x80 pixels
                marker.icon = icon

                // Set click listener to show place detail popup
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    showPlaceDetailPopupWithLoading(place)
                    true
                }

                binding.mapView.overlays.add(marker)
            }

            binding.mapView.invalidate()

            // Only show toast if plan type is not NONE
            val currentPlanType = viewModel.selectedPlanType.value
            if (currentPlanType != null && currentPlanType != PlanType.NONE) {
                if (places.isNotEmpty()) {
                    Toast.makeText(this, "Found ${places.size} ${currentPlanType.displayName}", Toast.LENGTH_SHORT).show()

                    // Center on first result if it's a search
                    places.firstOrNull()?.let {
                        binding.mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                    }
                } else {
                    Toast.makeText(this, "No places found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error.isNotBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            showLoadingOverlay(isLoading)
        }

        viewModel.selectedPlanType.observe(this) { planType ->
            // Update UI or show current selection
            // Toast.makeText(this, "Selected: ${planType.displayName}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show loading, fetch images, then show popup when ready
     */
    private fun showPlaceDetailPopupWithLoading(place: MapPlace) {
        // Show loading overlay
        showLoadingOverlay(true)

        // Fetch images first, then show popup
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Fetch images using Google Custom Search API
                val imageUrls = withContext(Dispatchers.IO) {
                    imageSearchRepository.searchImages(place.name, count = 2)
                }
                
                // Hide loading
                showLoadingOverlay(false)
                
                // Now show the popup with images
                showPlaceDetailPopup(place, imageUrls)
            } catch (e: Exception) {
                android.util.Log.e("PlanSelectionActivity", "Error loading images: ${e.message}", e)
                
                // Hide loading and show popup without images
                showLoadingOverlay(false)
                showPlaceDetailPopup(place, emptyList())
            }
        }
    }

    /**
     * Show place detail popup with pre-loaded images
     */
    private fun showPlaceDetailPopup(place: MapPlace, imageUrls: List<String>) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_place_detail_popup, null)
        bottomSheetDialog.setContentView(view)

        // Find views
        val tvPlaceName = view.findViewById<TextView>(R.id.tvPlaceName)
        val tvPlaceDescription = view.findViewById<TextView>(R.id.tvPlaceDescription)
        val imgGallery1 = view.findViewById<ImageView>(R.id.imgGallery1)
        val imgGallery2 = view.findViewById<ImageView>(R.id.imgGallery2)
        val btnAddPlace = view.findViewById<Button>(R.id.btnAddPlace)

        // Set place name
        tvPlaceName.text = place.name

        // Set place description (use address if description is null)
        tvPlaceDescription.text = place.description ?: place.address ?: "No description available"

        // Store the first photo URL to pass to detail activity
        selectedPhotoUrl = if (imageUrls.isNotEmpty()) imageUrls[0] else null

        // Load images using Glide
        if (imageUrls.isNotEmpty()) {
            Glide.with(this@PlanSelectionActivity)
                .load(imageUrls[0])
                .centerCrop()
                .placeholder(R.drawable.bg_a)
                .error(R.drawable.bg_a)
                .into(imgGallery1)
        }
        if (imageUrls.size > 1) {
            Glide.with(this@PlanSelectionActivity)
                .load(imageUrls[1])
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.bg_a)
                .into(imgGallery2)
        }

        // Set add button click listener
        btnAddPlace.setOnClickListener {
            bottomSheetDialog.dismiss()
            openDetailActivity(place)
        }

        bottomSheetDialog.show()
    }

    /**
     * Open appropriate detail activity based on selected plan type
     */
    private fun openDetailActivity(place: MapPlace) {
        val intent = when (viewModel.selectedPlanType.value) {
            PlanType.RESTAURANT -> Intent(this, RestaurantDetailActivity::class.java)
            PlanType.LODGING -> Intent(this, LodgingDetailActivity::class.java)
            PlanType.FLIGHT -> Intent(this, FlightDetailActivity::class.java)
            PlanType.BOAT -> Intent(this, BoatDetailActivity::class.java)
            PlanType.CAR_RENTAL -> Intent(this, CarRentalDetailActivity::class.java)
            PlanType.TRAIN -> Intent(this, CarRentalDetailActivity::class.java)
            PlanType.ACTIVITY, PlanType.TOUR, PlanType.THEATER, PlanType.SHOPPING,
            PlanType.CAMPING, PlanType.RELIGION -> Intent(this, ActivityDetailActivity::class.java)
            PlanType.NONE, null -> {
                Toast.makeText(this, "Please select a plan type first", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val selectedPlanType = viewModel.selectedPlanType.value
        intent.putExtra(EXTRA_TRIP_ID, tripId)
        intent.putExtra(EXTRA_PLACE_NAME, place.name)
        intent.putExtra(EXTRA_PLACE_ADDRESS, place.address)
        intent.putExtra(EXTRA_PLACE_LATITUDE, place.latitude)
        intent.putExtra(EXTRA_PLACE_LONGITUDE, place.longitude)
        intent.putExtra(EXTRA_PHOTO_URL, selectedPhotoUrl)
        intent.putExtra(EXTRA_PLAN_TYPE, selectedPlanType?.name)
        startActivity(intent)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_PLACE_NAME = "placeName"
        const val EXTRA_PLACE_ADDRESS = "placeAddress"
        const val EXTRA_PLACE_LATITUDE = "placeLatitude"
        const val EXTRA_PLACE_LONGITUDE = "placeLongitude"
        const val EXTRA_PHOTO_URL = "photoUrl"
        const val EXTRA_PLAN_TYPE = "planType"
    }

    /**
     * Check and request location permission
     */
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    /**
     * Get current location
     */
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude

                    // Update map center
                    binding.mapView.controller.setCenter(GeoPoint(currentLatitude, currentLongitude))

                    // Reload places with new location
                    val currentPlanType = binding.semiCircleMenu.getSelectedPlanType()
                    viewModel.selectPlanType(currentPlanType, currentLatitude, currentLongitude)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Show/hide loading overlay and disable/enable user interactions
     */
    private fun showLoadingOverlay(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        
        // Disable/enable interactions when loading
        binding.btnBack.isEnabled = !show
        binding.searchView.isEnabled = !show
        binding.semiCircleMenu.isEnabled = !show
        binding.mapView.isEnabled = !show
    }
}
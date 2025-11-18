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
import com.datn.apptravel.databinding.ActivityPlanSelectionBinding
import com.datn.apptravel.ui.trip.detail.plandetail.ActivityDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.BoatDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.CarRentalDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.FlightDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.LodgingDetailActivity
import com.datn.apptravel.ui.trip.detail.plandetail.RestaurantDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.PlanViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class PlanSelectionActivity : AppCompatActivity() {
    
    private val viewModel: PlanViewModel by viewModel()
    private var tripId: String? = null
    private lateinit var binding: ActivityPlanSelectionBinding
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocationOverlay: MyLocationNewOverlay? = null
    
    private var currentLatitude = 21.0285
    private var currentLongitude = 105.8542
    
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
    
    /**
     * Setup the map
     */
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
    
    /**
     * Set up the UI elements and click listeners
     */
    private fun setupUI() {
        // Set up back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        // Set up semi-circle menu
        binding.semiCircleMenu.setOnPlanSelectedListener { planType ->
            // When plan type changes, reload places with current search query (if any)
            viewModel.selectPlanType(planType, currentLatitude, currentLongitude)
        }
        
        // Set up search view
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()) {
                        // Search location and filter by selected plan type
                        viewModel.searchPlaces(it, currentLatitude, currentLongitude)
                        
                        // Hide keyboard
                        binding.searchView.clearFocus()
                    }
                }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // Clear search when text is empty
                if (newText.isNullOrBlank()) {
                    viewModel.clearSearch(currentLatitude, currentLongitude)
                }
                return true
            }
        })
        
        // Don't load initial data until we have GPS location
        // Will be loaded in getCurrentLocation() after GPS is ready
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
                
                // Set click listener to show place detail popup
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    showPlaceDetailPopup(place)
                    true
                }
                
                binding.mapView.overlays.add(marker)
            }
            
            binding.mapView.invalidate()
            
            if (places.isNotEmpty()) {
                val planType = viewModel.selectedPlanType.value?.displayName ?: "places"
                Toast.makeText(this, "Found ${places.size} $planType", Toast.LENGTH_SHORT).show()
                
                // Center on first result if it's a search
                places.firstOrNull()?.let {
                    binding.mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                }
            } else {
                Toast.makeText(this, "No places found", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.errorMessage.observe(this) { error ->
            if (error.isNotBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.selectedPlanType.observe(this) { planType ->
            // Update UI or show current selection
            // Toast.makeText(this, "Selected: ${planType.displayName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show place detail popup with image, description and add button
     */
    private fun showPlaceDetailPopup(place: MapPlace) {
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
        
        // Load gallery images
        place.galleryImages?.let { images ->
            if (images.isNotEmpty()) {
                Glide.with(this)
                    .load(images[0])
                    .centerCrop()
                    .into(imgGallery1)
            }
            if (images.size > 1) {
                Glide.with(this)
                    .load(images[1])
                    .centerCrop()
                    .into(imgGallery2)
            }
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
            PlanType.ACTIVITY, PlanType.TOUR, PlanType.THEATER, PlanType.SHOPPING, 
            PlanType.CAMPING, PlanType.RELIGION -> Intent(this, ActivityDetailActivity::class.java)
            else -> {
                Toast.makeText(this, "Please select a plan type first", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        intent.putExtra(EXTRA_TRIP_ID, tripId)
        intent.putExtra(EXTRA_PLACE_NAME, place.name)
        intent.putExtra(EXTRA_PLACE_ADDRESS, place.address)
        intent.putExtra(EXTRA_PLACE_LATITUDE, place.latitude)
        intent.putExtra(EXTRA_PLACE_LONGITUDE, place.longitude)
        startActivity(intent)
    }
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val EXTRA_TRIP_ID = "tripId"
        const val EXTRA_PLACE_NAME = "placeName"
        const val EXTRA_PLACE_ADDRESS = "placeAddress"
        const val EXTRA_PLACE_LATITUDE = "placeLatitude"
        const val EXTRA_PLACE_LONGITUDE = "placeLongitude"
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
        binding.mapView.onDetach()
    }
}
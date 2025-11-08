package com.datn.apptravel.ui.planselection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.datn.apptravel.R
import com.datn.apptravel.data.model.PlanType
import com.datn.apptravel.databinding.ActivityPlanSelectionBinding
import com.datn.apptravel.ui.viewmodel.PlanViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    
    private var currentLatitude = 21.0285 // Default to Hanoi
    private var currentLongitude = 105.8542
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    
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
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        viewModel.selectedPlanType.observe(this) { planType ->
            // Update UI or show current selection
            // Toast.makeText(this, "Selected: ${planType.displayName}", Toast.LENGTH_SHORT).show()
        }
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
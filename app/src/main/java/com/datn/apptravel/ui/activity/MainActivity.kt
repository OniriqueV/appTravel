package com.datn.apptravel.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ActivityMainBinding
import com.datn.apptravel.ui.base.BaseActivity
import com.datn.apptravel.ui.discover.DiscoverFragment
import com.datn.apptravel.ui.notification.NotificationFragment
import com.datn.apptravel.ui.profile.ProfileFragment
import com.datn.apptravel.ui.trip.TripsFragment
import com.datn.apptravel.ui.app.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {
    
    override val viewModel: MainViewModel by viewModel()
    
    private var currentTripsFragment: TripsFragment? = null
    
    override fun getViewBinding(): ActivityMainBinding = 
        ActivityMainBinding.inflate(layoutInflater)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set default fragment
        if (savedInstanceState == null) {
            val tripsFragment = TripsFragment()
            currentTripsFragment = tripsFragment
            replaceFragment(tripsFragment)
        }
    }
    
    override fun setupUI() {
        setupBottomNavigation()
        observeLoginStatus()
    }
    
    private fun observeLoginStatus() {
        viewModel.isUserLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                // User is not logged in, navigate to auth screen
                // TODO: Replace with actual SignIn activity once created
                // Commented out for now to prevent build errors
                // startActivity(Intent(this, SignInActivity::class.java))
                // finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh trips when returning from CreateTripActivity
        currentTripsFragment?.refreshTrips()
        
        // Check login status
        viewModel.checkLoginStatus()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trips -> {
                    val tripsFragment = TripsFragment()
                    currentTripsFragment = tripsFragment
                    replaceFragment(tripsFragment)
                    true
                }
                R.id.nav_notification -> {
                    replaceFragment(NotificationFragment())
                    currentTripsFragment = null
                    true
                }
                R.id.nav_add -> {
                    // Navigate to CreateTripActivity
                    val intent = Intent(this, com.datn.apptravel.ui.trip.CreateTripActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_discover -> {
                    replaceFragment(DiscoverFragment())
                    currentTripsFragment = null
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    currentTripsFragment = null
                    true
                }
                else -> false
            }
        }
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
    
    override fun handleLoading(isLoading: Boolean) {
        // Show loading indicator if needed
    }
}
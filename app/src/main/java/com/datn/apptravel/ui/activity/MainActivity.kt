package com.datn.apptravel.ui.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.datn.apptravel.R
import com.datn.apptravel.databinding.ActivityMainBinding
import com.datn.apptravel.ui.base.BaseActivity
import com.datn.apptravel.ui.discover.DiscoverFragment
import com.datn.apptravel.ui.notification.NotificationFragment
import com.datn.apptravel.ui.profile.ProfileFragment
import com.datn.apptravel.ui.trip.TripsFragment
import com.datn.apptravel.ui.app.MainViewModel
//import com.datn.apptravel.ui.discover.search.SearchExploreFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import com.datn.apptravel.ui.trip.CreateTripActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.badge.BadgeDrawable
import com.datn.apptravel.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    override val viewModel: MainViewModel by viewModel()

    private var currentTripsFragment: TripsFragment? = null
    private var notificationBadge: BadgeDrawable? = null
    private val notificationRepository: NotificationRepository by inject()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // BroadcastReceiver for notification updates
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.datn.apptravel.NOTIFICATION_RECEIVED") {
                Log.d("MainActivity", " Broadcast received: Firestore listener will auto-update badge")
                // Firestore snapshot listener will automatically detect changes
            }
        }
    }

    // Request notification permission for Android 13+
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun getViewBinding(): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        requestNotificationPermissionIfNeeded()

        // Handle window insets properly - add padding for status bar and navigation bar
//        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(0, systemBars.top, 0, 0)
//            insets
//        }

        // Set default fragment
        if (savedInstanceState == null) {
            val tripsFragment = TripsFragment()
            currentTripsFragment = tripsFragment
            replaceFragment(tripsFragment)
        }

        // Register broadcast receiver for notification updates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                notificationReceiver,
                IntentFilter("com.datn.apptravel.NOTIFICATION_RECEIVED"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                notificationReceiver,
                IntentFilter("com.datn.apptravel.NOTIFICATION_RECEIVED")
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", " Notification permission already granted")
                }
                else -> {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun setupUI() {
        setupBottomNavigation()
        observeLoginStatus()
        setupNotificationListener()
    }

    private fun setupNotificationListener() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MainActivity", "User not logged in - cannot setup listener")
            return
        }
        Log.d("MainActivity", " Setting up Firestore real-time listener for userId: $userId")

        // Create Firestore snapshot listener
        listenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainActivity", "Listener failed: ${error.message}")
                    return@addSnapshotListener
                }

                val unreadCount = snapshot?.size() ?: 0
                Log.d("MainActivity", "Real-time update: $unreadCount unread notifications")
                updateNotificationBadge(unreadCount)
            }
    }

    private fun observeLoginStatus() {
        viewModel.isUserLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                // TODO: navigate to login later
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        
        // Remove Firestore listener
        listenerRegistration?.remove()
    }

    private fun setupBottomNavigation() {
        // Setup notification badge
        notificationBadge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_notification)
        notificationBadge?.backgroundColor = getColor(R.color.red)
        notificationBadge?.badgeTextColor = getColor(R.color.white)
        
        // FAB Add button
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CreateTripActivity::class.java))
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trips -> {
                    replaceFragment(TripsFragment())
                    true
                }
                R.id.nav_notification -> {
                    replaceFragment(NotificationFragment())
                    true
                }


                R.id.nav_discover -> {
                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""  // hoặc FirebaseAuth.getInstance().currentUser?.uid
                    replaceFragment(
                        fragment = DiscoverFragment()
                    )

                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
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

    fun updateNotificationBadge(count: Int) {
        notificationBadge?.let { badge ->
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        }
    }

    override fun handleLoading(isLoading: Boolean) {}
}

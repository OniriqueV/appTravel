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
import com.datn.apptravel.ui.trip.CreateTripActivity
import com.google.android.material.badge.BadgeDrawable
import com.datn.apptravel.data.repository.NotificationRepository
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    override val viewModel: MainViewModel by viewModel()

    private var currentTripsFragment: TripsFragment? = null
    private var notificationBadge: BadgeDrawable? = null
    private val notificationRepository: NotificationRepository by inject()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.datn.apptravel.NOTIFICATION_RECEIVED") {
                Log.d("MainActivity", "Broadcast received: Firestore listener will auto-update badge")
            }
        }
    }

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
        requestNotificationPermissionIfNeeded()

        if (savedInstanceState == null) {
            val tripsFragment = TripsFragment()
            currentTripsFragment = tripsFragment
            replaceFragment(tripsFragment)
        }
        intent?.let { handleOpenProfile(it) }
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

    override fun setupUI() {
        setupBottomNavigation()
        observeLoginStatus()
        setupNotificationListener()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupNotificationListener() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("MainActivity", "User not logged in - cannot setup listener")
            return
        }

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
        currentTripsFragment?.refreshTrips()
        viewModel.checkLoginStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // ignore
        }
        listenerRegistration?.remove()
    }

    private fun setupBottomNavigation() {
        notificationBadge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_notification)
        notificationBadge?.backgroundColor = getColor(R.color.red)
        notificationBadge?.badgeTextColor = getColor(R.color.white)

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, CreateTripActivity::class.java))
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trips -> {
                    val f = TripsFragment()
                    currentTripsFragment = f
                    replaceFragment(f)
                    true
                }
                R.id.nav_notification -> {
                    replaceFragment(NotificationFragment())
                    true
                }
                R.id.nav_discover -> {
                    replaceFragment(DiscoverFragment())
                    true
                }
                R.id.nav_profile -> {
                    // ✅ profile của CHÍNH MÌNH
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * ✅ MỞ PROFILE CỦA NGƯỜI KHÁC (profileFollow/UserProfileFragment)
     * - Không dùng NavController
     * - Dùng FragmentManager replace theo kiến trúc hiện tại
     */
    fun openUserProfile(userId: String) {
        val fragment = com.datn.apptravel.ui.discover.profileFollow.UserProfileFragment().apply {
            arguments = Bundle().apply { putString("userId", userId) }
        }
        replaceFragment(fragment)
        // Không ép selected tab = profile vì tab profile là của mình
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null) // ✅ back quay lại feed
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
    private fun handleOpenProfile(intent: Intent) {
        val userId = intent.getStringExtra("open_profile_user_id") ?: return
        openUserProfile(userId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenProfile(intent)
    }

}

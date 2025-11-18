package com.datn.apptravel.ui.trip

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.databinding.FragmentTripsBinding
import com.datn.apptravel.ui.base.BaseFragment
import com.datn.apptravel.ui.trip.adapter.TripAdapter
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravel.ui.trip.viewmodel.TripsViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TripsFragment : BaseFragment<FragmentTripsBinding, TripsViewModel>() {

    override val viewModel: TripsViewModel by viewModel()
    private val authRepository: AuthRepository by inject()

    // Track currently selected tab
    private var currentTab = TAB_ONGOING

    companion object {
        private const val TAB_ONGOING = 0
        private const val TAB_PAST = 1
        private const val TAB_COMMUNITY = 2

        // Intent extra keys
        const val EXTRA_TRIP_ID = "extra_trip_id"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTripsBinding =
        FragmentTripsBinding.inflate(inflater, container, false)

    // Trip adapter
    private lateinit var tripAdapter: TripAdapter

    override fun setupUI() {
        // Setup UI components
        loadUserName()
        setupAddTripButton()
        setupRecyclerViews()
        observeTrips()
        loadTrips()
    }

    /**
     * Load and display user name
     */
    private fun loadUserName() {
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                user?.let {
                    val displayName = if (it.lastName.isNotEmpty()) {
                        it.lastName
                    } else if (it.firstName.isNotEmpty()) {
                        it.firstName
                    } else {
                        "User"
                    }
                    binding.tvUserName.text = displayName
                }
            }
        }
    }

    private fun setupAddTripButton() {
        binding.btnAddTripNow.setOnClickListener {
            // Navigate to CreateTripActivity
            val intent = Intent(requireContext(), CreateTripActivity::class.java)
            startActivity(intent)
        }

        binding.tvViewAll?.setOnClickListener {
            // Show all past trips
        }
    }

    /**
     * Load trips data based on current tab
     */
    private fun loadTrips() {
        viewModel.getTrips()
    }

    /**
     * Refresh trips data
     */
    fun refreshTrips() {
        viewModel.getTrips()
    }

    /**
     * Setup RecyclerViews for Adventure and Past Trips
     */
    private fun setupRecyclerViews() {
        // Setup Adventure RecyclerView (horizontal)
        binding.rvAdventure.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Setup Past Trips RecyclerView (horizontal)
        tripAdapter = TripAdapter(
            emptyList()
        ) { trip ->
            navigateToTripDetail(trip)
        }

        binding.rvPastTrips.apply {
            adapter = tripAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    /**
     * Navigate to trip detail screen
     */
    private fun navigateToTripDetail(trip: Trip) {
        val intent = Intent(requireContext(), TripDetailActivity::class.java).apply {
            putExtra(EXTRA_TRIP_ID, trip.id.toString())
        }
        startActivity(intent)
    }

    /**
     * Observe trips data from ViewModel
     */
    private fun observeTrips() {
        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            // If trips exist, update the adapter and show RecyclerView
            if (trips.isNotEmpty()) {
                tripAdapter.updateTrips(trips)
                binding.rvPastTrips.visibility = View.VISIBLE
            } else {
                binding.rvPastTrips.visibility = View.GONE
            }
        }
    }

    override fun handleLoading(isLoading: Boolean) {
        // Show/hide loading indicator
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
package com.datn.apptravel.ui.discover.feed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.datn.apptravel.R
import com.datn.apptravel.ui.activity.MainActivity
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.adapter.DiscoverFeedAdapter
import com.datn.apptravel.ui.discover.network.FollowRepository
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class FollowingFragment : Fragment(), Refreshable {

    private val viewModel: DiscoverViewModel by viewModel()
    private val followRepository: FollowRepository by inject()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DiscoverFeedAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_following_feed, container, false)
        recycler = view.findViewById(R.id.recyclerFollowing)
        progressBar = view.findViewById(R.id.progressFollowing)
        swipeRefresh = view.findViewById(R.id.swipeRefreshFollowing)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        adapter = DiscoverFeedAdapter(
            currentUserId = currentUserId,
            items = mutableListOf(),
            followRepository = followRepository,
            lifecycleOwner = viewLifecycleOwner,

            onTripClick = { tripId ->
                val intent = Intent(requireContext(), TripDetailActivity::class.java)
                intent.putExtra("tripId", tripId)
                intent.putExtra("READ_ONLY", true)
                startActivity(intent)
            },

            onUserClick = { userId ->
                (activity as? MainActivity)?.openUserProfile(userId)
            },

            onFollowChanged = { _, _ -> }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener { onRefresh() }
        observeViewModel()
        onRefresh()
    }

    private fun observeViewModel() {
        viewModel.followingList.observe(viewLifecycleOwner) {
            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            adapter.submitList(it)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            swipeRefresh.isRefreshing = false
            if (!it.isNullOrBlank()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRefresh() {
        progressBar.visibility = View.VISIBLE
        viewModel.loadFollowing()
    }
}

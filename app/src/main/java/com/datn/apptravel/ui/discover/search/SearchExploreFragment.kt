package com.datn.apptravel.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravel.databinding.FragmentSearchExploreBinding
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
import com.datn.apptravel.ui.discover.profileFollow.UserProfileFragment
import com.datn.apptravel.R
import com.datn.apptravel.data.api.RetrofitClient
import com.datn.apptravel.ui.discover.search.adapter.SearchAdapter
import com.datn.apptravel.ui.search.network.SearchApi
import com.datn.apptravel.ui.trip.map.TripMapActivity


class SearchExploreFragment : Fragment(R.layout.fragment_search_explore) {

    private lateinit var binding: FragmentSearchExploreBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentSearchExploreBinding.bind(view)

        val api = RetrofitClient.createDiscoverService(SearchApi::class.java)
        val repo = SearchRepository(api)
        viewModel = ViewModelProvider(this, SearchVMFactory(repo))[SearchViewModel::class.java]

        adapter = SearchAdapter(
            onUserClick = { openUser(it) },
            onTripClick = { tripId -> openTripDetail(tripId) }
        )

        binding.rvExplore.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExplore.adapter = adapter

        binding.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(binding.etSearch.text.toString())
                true
            } else false
        }

        viewModel.items.observe(viewLifecycleOwner) {
            adapter.submit(it)
        }
    }

    private fun openUser(userId: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, UserProfileFragment().apply {
                arguments = Bundle().apply { putString("userId", userId) }
            })
            .addToBackStack(null)
            .commit()
    }

    private fun openTripDetail(tripId: String) {
        val intent = Intent(requireContext(), TripMapActivity::class.java)
        intent.putExtra("tripId", tripId)
        startActivity(intent)
    }
}

package com.datn.apptravel.ui.discover.feed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.detail.PostDetailActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.datn.apptravel.ui.discover.adapter.DiscoverFeedAdapter
import com.datn.apptravel.ui.discover.Refreshable


class FollowingFragment : Fragment(), Refreshable  {

    private val viewModel: DiscoverViewModel by viewModel()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DiscoverFeedAdapter

    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var currentUserId = ""

    override fun onRefresh() {
        viewModel.loadDiscover()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserId = arguments?.getString("userId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_following_feed, container, false)

        recycler = v.findViewById(R.id.recyclerFollowing)
        progressBar = v.findViewById(R.id.progressFollowing)
        swipeRefresh = v.findViewById(R.id.swipeRefreshFollowing)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiscoverFeedAdapter { item ->
            openPostDetail(item)
        }
        recycler.adapter = adapter

        observeViewModel()
        loadData()

        swipeRefresh.setOnRefreshListener { loadData() }
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

    private fun loadData() {
        if (currentUserId.isBlank()) {
            Toast.makeText(requireContext(), "Thiếu userId", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = View.VISIBLE
        viewModel.loadFollowing(currentUserId, 0, 20)
    }

    private fun openPostDetail(item: DiscoverItem) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java)
        intent.putExtra("postId", item.postId)
        intent.putExtra("userId", currentUserId)
        startActivity(intent)
    }

    companion object {
        fun newInstance(userId: String) = FollowingFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
    }
}

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
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.detail.PostDetailActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.datn.apptravel.ui.discover.adapter.DiscoverFeedAdapter

class RandomFeedFragment : Fragment(), Refreshable {

    private val viewModel: DiscoverViewModel by viewModel()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DiscoverFeedAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onRefresh() {
        viewModel.loadDiscover()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_random_feed, container, false)

        recycler = v.findViewById(R.id.recyclerDiscover)
        progressBar = v.findViewById(R.id.progressDiscover)
        swipeRefresh = v.findViewById(R.id.swipeRefreshDiscover)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiscoverFeedAdapter { post ->
            // 👉 mở PostDetail
            val intent = Intent(requireContext(), PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)
            startActivity(intent)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        observeViewModel()
        viewModel.loadDiscover()
    }
    private fun observeViewModel() {
        viewModel.discoverList.observe(viewLifecycleOwner) { list ->
            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            adapter.submitList(list)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            swipeRefresh.isRefreshing = false
            if (!it.isNullOrEmpty()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        viewModel.loadDiscover(0, 20, "newest")
    }

    private fun openPostDetail(item: DiscoverItem) {
        val i = Intent(requireContext(), PostDetailActivity::class.java)
        i.putExtra("postId", item.postId)
        startActivity(i)
    }
}


package com.datn.apptravel.ui.discover.feed

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.Refreshable
import com.datn.apptravel.ui.discover.adapter.DiscoverFeedAdapter
import com.datn.apptravel.ui.discover.detail.PostDetailActivity
import com.datn.apptravel.ui.discover.model.DiscoverItem
import org.koin.androidx.viewmodel.ext.android.viewModel

class RandomFeedFragment : Fragment(), Refreshable {

    private val viewModel: DiscoverViewModel by viewModel()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DiscoverFeedAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_random_feed, container, false)

        recycler = v.findViewById(R.id.recyclerDiscover)
        progressBar = v.findViewById(R.id.progressDiscover)
        swipeRefresh = v.findViewById(R.id.swipeRefreshDiscover)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiscoverFeedAdapter(
            onPostClick = { item -> openPostDetail(item) },
            onComment = { postId -> openPostDetailById(postId) }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            onRefresh()
        }

        observeViewModel()
        onRefresh()
    }

    // ===== Activity Result =====
    private val postDetailLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onRefresh()
            }
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

    private fun openPostDetail(item: DiscoverItem) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
            putExtra(PostDetailActivity.EXTRA_POST_ID, item.postId)
        }
        postDetailLauncher.launch(intent)
    }

    private fun openPostDetailById(postId: String) {
        val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
            putExtra(PostDetailActivity.EXTRA_POST_ID, postId)
            putExtra(PostDetailActivity.EXTRA_OPEN_COMMENT, true)
        }
        postDetailLauncher.launch(intent)
    }

    // ===== Refreshable =====
    override fun onRefresh() {
        progressBar.visibility = View.VISIBLE
        viewModel.loadDiscover()
    }
}

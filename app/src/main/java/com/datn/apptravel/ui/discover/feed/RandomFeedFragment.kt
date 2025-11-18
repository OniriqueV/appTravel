package com.datn.apptravel.ui.discover.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.adapter.PostAdapter

class RandomFeedFragment : Fragment() {

    private lateinit var viewModel: DiscoverViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_random_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DiscoverViewModel::class.java]

        recyclerView = view.findViewById(R.id.rvRandomPosts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PostAdapter(
            items = emptyList(),
            onLikeClicked = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClicked = { post ->
                // TODO mở comment sau
            }
        )
        recyclerView.adapter = adapter

        viewModel.randomPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }
    }
}
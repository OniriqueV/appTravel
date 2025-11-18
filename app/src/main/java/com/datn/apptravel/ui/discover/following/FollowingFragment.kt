package com.datn.apptravel.ui.discover.following

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

class FollowingFragment : Fragment() {

    private lateinit var viewModel: DiscoverViewModel
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_following_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DiscoverViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerFollowing)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PostAdapter(
            items = emptyList(),
            onLikeClicked = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClicked = { post ->
                // TODO: handle comment click
            }
        )

        recyclerView.adapter = adapter

        viewModel.followingPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }
    }
}

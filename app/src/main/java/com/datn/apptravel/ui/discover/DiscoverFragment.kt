package com.datn.apptravel.ui.discover

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.datn.apptravel.databinding.FragmentDiscoverBinding
import com.datn.apptravel.ui.discover.adapter.DiscoverPagerAdapter
import com.datn.apptravel.ui.discover.post.CreatePostActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.search.SearchExploreFragment


class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ LẤY USERID CHUẨN
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupEvents()
    }

    private fun setupViewPager() {
        binding.vpDiscover.adapter = DiscoverPagerAdapter(
            fragment = this,
            userId = userId
        )

        TabLayoutMediator(binding.tabDiscover, binding.vpDiscover) { tab, position ->
            tab.text = if (position == 0) "Explore" else "Following"
        }.attach()
    }

    private fun setupEvents() {
        binding.layoutSearch.setOnClickListener {
            val f = SearchExploreFragment().apply {
                arguments = Bundle().apply { putString("userId", userId) }
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, f)
                .addToBackStack("search")
                .commit()
        }


        // ➕ Create Post
        binding.btnCreatePost.setOnClickListener {
            if (userId.isBlank()) {
                Toast.makeText(requireContext(), "Thiếu userId", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(requireContext(), CreatePostActivity::class.java)
            i.putExtra("userId", userId)
            createPostLauncher.launch(i)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val createPostLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // ✅ Post mới được tạo → refresh Discover
                refreshDiscover()
            }
        }

    private fun refreshDiscover() {
        val adapter = binding.vpDiscover.adapter as? DiscoverPagerAdapter
        adapter?.refresh()
    }

}

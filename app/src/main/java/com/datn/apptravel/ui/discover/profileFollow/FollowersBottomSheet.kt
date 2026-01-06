package com.datn.apptravels.ui.discover.profileFollow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravels.databinding.BottomSheetFollowersBinding
import com.datn.apptravels.ui.discover.network.ProfileRepository
import com.datn.apptravels.ui.discover.profileFollow.adapter.FollowersAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class FollowersBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFollowersBinding? = null
    private val binding get() = _binding!!

    private val profileRepository: ProfileRepository by inject()
    private lateinit var adapter: FollowersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFollowersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString(ARG_USER_ID) ?: return

        adapter = FollowersAdapter(mutableListOf()) { clickedUserId ->
            (activity as? com.datn.apptravels.ui.activity.MainActivity)
                ?.openUserProfile(clickedUserId)
            dismiss()
        }

        binding.recyclerFollowers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFollowers.adapter = adapter

        lifecycleScope.launch {
            val followers = profileRepository.getFollowers(userId)
            adapter.submitList(followers)
            binding.tvTitle.text = "Followers (${followers.size})"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "userId"

        fun newInstance(userId: String): FollowersBottomSheet {
            return FollowersBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }
        }
    }
}

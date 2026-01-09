package com.datn.apptravels.ui.discover.profileFollow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.databinding.FragmentUserProfileBinding
import com.datn.apptravels.ui.discover.network.FollowRepository
import com.datn.apptravels.ui.discover.network.ProfileRepository
import com.datn.apptravels.ui.discover.profileFollow.adapter.ProfileTripAdapter
import com.datn.apptravels.ui.discover.util.ImageUrlUtil
import com.datn.apptravels.ui.trip.TripsFragment
import com.datn.apptravels.ui.trip.detail.tripdetail.TripDetailActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class UserProfileFragment : Fragment() {

    private lateinit var tripAdapter: ProfileTripAdapter
    private val viewModel: ProfileUserViewModel by viewModel()

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val followRepository: FollowRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    private lateinit var targetUserId: String
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private var isSelfProfile: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = requireArguments().getString("userId")!!
        isSelfProfile = requireArguments().getBoolean("isSelfProfile", false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        setupRecycler()
        observeViewModel()
        checkFollowState()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewModel.loadTrips(
            userId = targetUserId,
            viewerId = currentUserId
        )
    }

    // 🔥 QUAN TRỌNG: quay lại màn hình là reload profile
    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    // ================= PROFILE =================
    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val profile = profileRepository.getProfile(targetUserId)

                binding.tvUserName.text = profile.userName
                binding.tvTime.text = "${profile.followerCount} followers"
                binding.tvTime.visibility = View.VISIBLE

                binding.tvTime.setOnClickListener {
                    FollowersBottomSheet
                        .newInstance(targetUserId)
                        .show(parentFragmentManager, "followers")
                }

                Glide.with(this@UserProfileFragment)
                    .load(ImageUrlUtil.toFullUrl(profile.userAvatar))
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.imgAvatar)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ================= FOLLOW STATE =================
    private fun checkFollowState() {
        val me = currentUserId ?: return
        // Hide follow button if viewing own profile
        if (isSelfProfile || me == targetUserId) {
            binding.btnFollow.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val isFollowing = followRepository.isFollowing(me, targetUserId)
            renderFollowButton(isFollowing)
        }
    }

    private fun renderFollowButton(isFollowing: Boolean) {
        // Hide follow button if viewing own profile
        if (isSelfProfile || currentUserId == targetUserId) {
            binding.btnFollow.visibility = View.GONE
            return
        }

        if (isFollowing) {
            binding.btnFollow.text = "Following"
            binding.btnFollow.setOnClickListener { showUnfollowConfirm() }
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.setOnClickListener { follow() }
        }
    }

    private fun showUnfollowConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("Bỏ theo dõi?")
            .setMessage("Bạn sẽ không còn thấy bài viết của người này.")
            .setPositiveButton("Bỏ theo dõi") { _, _ -> unfollow() }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun follow() {
        val me = currentUserId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                followRepository.follow(me, targetUserId)
                renderFollowButton(true)
                loadProfile()
            } catch (_: Exception) {}
        }
    }

    private fun unfollow() {
        val me = currentUserId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                followRepository.unfollow(me, targetUserId)
            } finally {
                renderFollowButton(false)
                loadProfile()
            }
        }
    }

    // ================= TRIPS =================
    private fun setupRecycler() {
        tripAdapter = ProfileTripAdapter(mutableListOf()) { tripId ->
            val intent = Intent(requireContext(), TripDetailActivity::class.java)
            intent.putExtra(TripsFragment.EXTRA_TRIP_ID, tripId)
            startActivity(intent)
        }

        binding.recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTrips.adapter = tripAdapter
    }

    private fun observeViewModel() {
        viewModel.trips.observe(viewLifecycleOwner) {
            tripAdapter.submitList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

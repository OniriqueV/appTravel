package com.datn.apptravel.ui.discover.profileFollow

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
import com.datn.apptravel.R
import com.datn.apptravel.databinding.FragmentUserProfileBinding
import com.datn.apptravel.ui.discover.network.FollowRepository
import com.datn.apptravel.ui.discover.profile.ProfileTripAdapter
import com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity
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

    private lateinit var targetUserId: String
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = requireArguments().getString("userId")!!
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

        setupUserInfo()
        setupRecycler()
        observeViewModel()
        checkFollowState()

        // 🔥 LOAD TRIPS
        viewModel.loadTrips(
            profileUserId = targetUserId,
            viewerId = currentUserId
        )
    }

    // ================= USER INFO =================
    private fun setupUserInfo() {
        binding.tvUserName.text = "Tom Hank" // TODO load từ API

        Glide.with(this)
            .load(R.drawable.ic_avatar_placeholder) // ✅ KHÔNG load null
            .circleCrop() // ✅ Dùng API chuẩn
            .into(binding.imgAvatar)
    }

    // ================= FOLLOW STATE =================
    private fun checkFollowState() {
        val me = currentUserId ?: return
        if (me == targetUserId) {
            binding.btnFollow.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val isFollowing = followRepository.isFollowing(me, targetUserId)
            renderFollowButton(isFollowing)
        }
    }

    private fun renderFollowButton(isFollowing: Boolean) {
        if (currentUserId == targetUserId) {
            binding.btnFollow.visibility = View.GONE
            return
        }

        if (isFollowing) {
            binding.btnFollow.text = "Following"
            binding.btnFollow.isEnabled = true
            binding.btnFollow.setOnClickListener { showUnfollowConfirm() }
        } else {
            binding.btnFollow.text = "Follow"
            binding.btnFollow.isEnabled = true
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
            } catch (e: Exception) {
                // show toast nếu cần
            }
        }
    }


    private fun unfollow() {
        val me = currentUserId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            followRepository.unfollow(me, targetUserId)
            renderFollowButton(false)
        }
    }




    // ================= TRIPS =================
    private fun setupRecycler() {
        tripAdapter = ProfileTripAdapter(mutableListOf()) { tripId ->
            val intent = Intent(requireContext(), TripDetailActivity::class.java)
            intent.putExtra("tripId", tripId)
            intent.putExtra("READ_ONLY", true)
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



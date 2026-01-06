package com.datn.apptravels.ui.discover.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.R
import com.datn.apptravels.ui.discover.network.FollowRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddFollowerBottomSheet(
    private val userId: String,
    private val onUserSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private val followRepository: FollowRepository by inject()

    private lateinit var rvFollower: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_add_follower, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFollower = view.findViewById(R.id.rvFollower)
        progress = view.findViewById(R.id.progressFollower)
        tvEmpty = view.findViewById(R.id.tvEmptyFollower)

        val adapter = FollowerAdapter { user ->
            user.userId?.let {
                onUserSelected(it)
                dismiss()
            }
        }


        rvFollower.layoutManager = LinearLayoutManager(requireContext())
        rvFollower.adapter = adapter

        loadFollowers(adapter)
    }

    private fun loadFollowers(adapter: FollowerAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            progress.isVisible = true
            try {
                val list = followRepository.getFollowersRaw(userId) // ✅ DÙNG FIELD
                adapter.submit(list)
                tvEmpty.isVisible = list.isEmpty()
            } catch (e: Exception) {
                tvEmpty.isVisible = true
            } finally {
                progress.isVisible = false
            }
        }
    }

}

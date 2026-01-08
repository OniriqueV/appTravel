package com.datn.apptravels.ui.profile.badges

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.datn.apptravels.data.model.Badge
import com.datn.apptravels.data.model.BadgeDefinitions
import com.datn.apptravels.data.model.UserBadge
import com.datn.apptravels.data.repository.BadgeRepository
import com.datn.apptravels.data.repository.StatisticsRepository
import com.datn.apptravels.databinding.ActivityBadgesBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BadgesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBadgesBinding
    private val badgeRepository: BadgeRepository by inject()
    private val statisticsRepository: StatisticsRepository by inject()
    private val auth: FirebaseAuth by inject()

    private lateinit var earnedAdapter: BadgeAdapter
    private lateinit var lockedAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBadgesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAndLoadBadges()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        earnedAdapter = BadgeAdapter(true) // Show earned badges
        binding.recyclerViewEarnedBadges.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewEarnedBadges.adapter = earnedAdapter

        lockedAdapter = BadgeAdapter(false) // Show locked badges
        binding.recyclerViewLockedBadges.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewLockedBadges.adapter = lockedAdapter
    }

    private fun checkAndLoadBadges() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Get statistics first
            val statsResult = statisticsRepository.getUserStatistics(userId)

            statsResult.onSuccess { statistics ->
                // Check for new badges
                badgeRepository.checkAndAwardBadges(userId, statistics)

                // Load all badges
                loadBadges(userId)
            }.onFailure {
                binding.progressBar.visibility = View.GONE
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun loadBadges(userId: String) {
        lifecycleScope.launch {
            val result = badgeRepository.getUserBadges(userId)

            binding.progressBar.visibility = View.GONE

            result.onSuccess { userBadges ->
                val allBadges = BadgeDefinitions.getAllBadges()
                val earnedBadgeIds = userBadges.map { it.badgeId }.toSet()

                // Separate earned and locked badges
                val earnedBadges = allBadges.filter { it.id in earnedBadgeIds }
                val lockedBadges = allBadges.filter { it.id !in earnedBadgeIds }

                // Update UI
                binding.tvBadgeCount.text = "${earnedBadges.size} / ${allBadges.size}"

                earnedAdapter.submitList(earnedBadges)
                lockedAdapter.submitList(lockedBadges)

            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
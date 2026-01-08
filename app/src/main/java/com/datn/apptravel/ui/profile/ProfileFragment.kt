package com.datn.apptravels.ui.profile

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.databinding.FragmentProfileBinding
import com.datn.apptravels.ui.activity.SignInActivity
import com.datn.apptravels.ui.base.BaseFragment
import com.datn.apptravels.ui.profile.badges.BadgesActivity
import com.datn.apptravels.ui.profile.documents.DocumentsActivity
import com.datn.apptravels.ui.profile.edit.EditProfileActivity
import com.datn.apptravels.ui.profile.password.ChangePasswordActivity
import com.datn.apptravels.ui.profile.statistics.StatisticsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    override val viewModel: ProfileViewModel by viewModel()

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadUserProfile()
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupToolbar()
        loadUserProfile()
        setupClickListeners()
        observeViewModel()
        checkNewBadges()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun loadUserProfile() {
        viewModel.getUserProfile()
    }

    private fun setupClickListeners() {
        // Edit profile
        binding.cardProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        // Statistics - NEW
        binding.layoutStatistics.setOnClickListener {
            val intent = Intent(requireContext(), StatisticsActivity::class.java)
            startActivity(intent)
        }

        // Badges - NEW
        binding.layoutBadges.setOnClickListener {
            val intent = Intent(requireContext(), BadgesActivity::class.java)
            startActivity(intent)
            // Reset badge indicator after viewing
            binding.tvNewBadgeCount.visibility = View.GONE
        }

        // Documents - UPDATED
        binding.layoutDocument.setOnClickListener {
            val intent = Intent(requireContext(), DocumentsActivity::class.java)
            startActivity(intent)
        }

        // Change password
        binding.layoutChangePassword.setOnClickListener {
            val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Load avatar
                Glide.with(this)
                    .load(it.profilePicture)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.ivAvatar)

                // Display user info
                binding.tvName.text = "${it.firstName} ${it.lastName}"
                binding.tvEmail.text = it.email

                // Show/hide change password based on provider
                binding.layoutChangePassword.visibility =
                    if (it.provider == "LOCAL") View.VISIBLE else View.GONE
            }
        }

        viewModel.logoutResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                navigateToSignIn()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                showToast(it)
            }
        }
    }

    // NEW: Check for new badges
    private fun checkNewBadges() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newBadgeCount = viewModel.getNewBadgeCount()
            if (newBadgeCount > 0) {
                binding.tvNewBadgeCount.text = newBadgeCount.toString()
                binding.tvNewBadgeCount.visibility = View.VISIBLE
            } else {
                binding.tvNewBadgeCount.visibility = View.GONE
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun navigateToSignIn() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun handleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh badge count when returning to fragment
        checkNewBadges()
    }
}
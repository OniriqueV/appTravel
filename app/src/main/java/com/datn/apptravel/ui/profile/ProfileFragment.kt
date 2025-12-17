package com.datn.apptravel.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.databinding.FragmentProfileBinding
import com.datn.apptravel.ui.aisuggest.AISuggestActivity
import com.datn.apptravel.ui.aisuggest.ExtendedAISuggestActivity
import com.datn.apptravel.ui.auth.SignInActivity
import com.datn.apptravel.ui.base.BaseFragment
import com.datn.apptravel.ui.profile.edit.EditProfileActivity
import com.datn.apptravel.ui.profile.password.ChangePasswordActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        // AI Suggest - Nút mới thêm vào
        binding.btnAISuggest.setOnClickListener {
            val intent = Intent(requireContext(), ExtendedAISuggestActivity::class.java)
            startActivity(intent)
        }

        // Edit profile
        binding.cardProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        // Document (coming soon)
        binding.layoutDocument.setOnClickListener {
            showToast("Chức năng đang được phát triển")
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
}
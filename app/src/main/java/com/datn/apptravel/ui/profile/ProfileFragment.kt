package com.datn.apptravel.ui.profile

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.datn.apptravel.databinding.FragmentProfileBinding
import com.datn.apptravel.ui.auth.SignInActivity
import com.datn.apptravel.ui.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {

    override val viewModel: ProfileViewModel by viewModel()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // Setup UI components
        loadUserProfile()
        setupLogoutButton()
        observeLogoutResult()
    }

    private fun loadUserProfile() {
        viewModel.getUserProfile()
    }

    private fun setupLogoutButton() {
        binding.btnLogout?.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeLogoutResult() {
        viewModel.logoutResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to SignInActivity and clear back stack
                val intent = Intent(requireContext(), SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    override fun handleLoading(isLoading: Boolean) {
        // Show/hide loading indicator
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
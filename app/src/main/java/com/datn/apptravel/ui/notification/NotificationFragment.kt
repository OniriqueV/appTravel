package com.datn.apptravel.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.datn.apptravel.databinding.FragmentNotificationBinding
import com.datn.apptravel.ui.base.BaseFragment
import com.datn.apptravel.ui.notification.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationFragment : BaseFragment<FragmentNotificationBinding, NotificationViewModel>() {

    override val viewModel: NotificationViewModel by viewModel()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotificationBinding =
        FragmentNotificationBinding.inflate(inflater, container, false)

    override fun setupUI() {
        // Setup UI components
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModel.getNotifications()
    }

    override fun handleLoading(isLoading: Boolean) {
        // Show/hide loading indicator
        binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
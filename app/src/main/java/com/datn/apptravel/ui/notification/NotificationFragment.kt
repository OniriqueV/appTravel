package com.datn.apptravels.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.datn.apptravels.R
import com.datn.apptravels.databinding.FragmentNotificationBinding
import com.datn.apptravels.ui.base.BaseFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotificationFragment : BaseFragment<FragmentNotificationBinding, NotificationViewModel>() {

    override val viewModel: NotificationViewModel by viewModel()
    private lateinit var pagerAdapter: NotificationPagerAdapter
    private val dots = mutableListOf<ImageView>()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotificationBinding =
        FragmentNotificationBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupViewPager()
        setupNotificationToggle()
        setupMarkAllAsRead()
        observeNotifications()
        observeNotificationSettings()
        observePagination()
        loadNotifications()
    }

    private fun setupViewPager() {
        pagerAdapter = NotificationPagerAdapter(
            onItemClick = { notification ->
                viewModel.markAsRead(notification.id)
            },
            onItemDelete = { notification ->
                viewModel.deleteNotification(notification.id)

            }
        )
        binding.viewPagerNotifications.adapter = pagerAdapter
        
        // Listen to page changes
        binding.viewPagerNotifications.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setPage(position)
                updateDotsIndicator(position)
            }
        })
    }

    private fun setupMarkAllAsRead() {
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()

        }
    }

    private fun setupNotificationToggle() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
            Toast.makeText(
                requireContext(),
                if (isChecked) "Đã bật thông báo" else "Đã tắt thông báo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeNotificationSettings() {
        lifecycleScope.launch {
            viewModel.notificationsEnabled.collect { enabled ->
                // Update switch without triggering listener
                binding.switchNotifications.setOnCheckedChangeListener(null)
                binding.switchNotifications.isChecked = enabled
                binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.toggleNotifications(isChecked)
                    Toast.makeText(
                        requireContext(),
                        if (isChecked) "Đã bật thông báo" else "Đã tắt thông báo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            viewModel.allNotifications.collect { allNotifications ->
                android.util.Log.d("NotificationFragment", "Received ${allNotifications.size} notifications from ViewModel")
                
                // Submit all notifications to pager adapter
                pagerAdapter.submitList(allNotifications)
                
                // Update empty state
                binding.layoutEmptyState.visibility = 
                    if (allNotifications.isEmpty()) View.VISIBLE else View.GONE
                binding.viewPagerNotifications.visibility = 
                    if (allNotifications.isEmpty()) View.GONE else View.VISIBLE
                
                // Update badge count
                val unreadCount = viewModel.getUnreadCount()
                android.util.Log.d("NotificationFragment", "Unread count: $unreadCount")
                (activity as? com.datn.apptravels.ui.activity.MainActivity)?.updateNotificationBadge(unreadCount)
            }
        }
    }

    private fun observePagination() {
        lifecycleScope.launch {
            // Observe total pages to update dots
            viewModel.totalPages.collect { totalPages ->
                setupDotsIndicator(totalPages)
            }
        }
        
        lifecycleScope.launch {
            // Observe current page
            viewModel.currentPage.collect { currentPage ->
                updateDotsIndicator(currentPage)
            }
        }
    }

    private fun setupDotsIndicator(totalPages: Int) {
        binding.dotsIndicator.removeAllViews()
        dots.clear()

        if (totalPages <= 1) {
            binding.dotsIndicator.visibility = View.GONE
            return
        }

        binding.dotsIndicator.visibility = View.VISIBLE

        for (i in 0 until totalPages) {
            val dot = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_circle)
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.dot_size),
                    resources.getDimensionPixelSize(R.dimen.dot_size)
                ).apply {
                    setMargins(
                        resources.getDimensionPixelSize(R.dimen.dot_margin),
                        0,
                        resources.getDimensionPixelSize(R.dimen.dot_margin),
                        0
                    )
                }
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey_400))
            }
            binding.dotsIndicator.addView(dot)
            dots.add(dot)
        }

        updateDotsIndicator(0)
    }

    private fun updateDotsIndicator(currentPage: Int) {
        dots.forEachIndexed { index, dot ->
            val color = if (index == currentPage) {
                ContextCompat.getColor(requireContext(), R.color.blue_primary)
            } else {
                ContextCompat.getColor(requireContext(), R.color.grey_400)
            }
            dot.setColorFilter(color)
        }
    }

    private fun loadNotifications() {
        android.util.Log.d("NotificationFragment", "Loading notifications...")
        viewModel.getNotifications()
    }

    override fun onResume() {
        super.onResume()
        // Reload notifications when returning to fragment
        viewModel.getNotifications()
    }

    override fun handleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
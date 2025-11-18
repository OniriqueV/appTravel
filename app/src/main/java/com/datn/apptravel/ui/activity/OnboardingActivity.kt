package com.datn.apptravel.ui.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.datn.apptravel.databinding.ActivityOnboardingBinding
import com.datn.apptravel.ui.base.BaseActivity
import com.datn.apptravel.ui.auth.OnboardingFragment
import com.datn.apptravel.ui.auth.OnboardingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding, OnboardingViewModel>() {

    override val viewModel: OnboardingViewModel by viewModel()

    private lateinit var pagerAdapter: OnboardingPagerAdapter

    override fun getViewBinding(): ActivityOnboardingBinding =
        ActivityOnboardingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
    }

    override fun setupUI() {
        setupViewPager()
        observeCurrentPosition()
    }

    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(this)

        binding.viewPagerOnboarding.apply {
            adapter = pagerAdapter
            // Loại bỏ transformer để tránh méo layout khi back
            setPageTransformer(null)
            offscreenPageLimit = 1

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.updatePosition(position)
                }
            })
        }
    }

    private fun observeCurrentPosition() {
        viewModel.currentPosition.observe(this) { position ->
            binding.viewPagerOnboarding.currentItem = position
        }
    }

    inner class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = viewModel.onboardingData.value?.size ?: 0

        override fun createFragment(position: Int): Fragment {
            return OnboardingFragment.newInstance(position)
        }
    }
}

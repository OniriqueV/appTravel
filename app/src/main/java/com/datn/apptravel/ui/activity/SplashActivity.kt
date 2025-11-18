package com.datn.apptravel.ui.activity

import android.content.Intent
import android.os.Bundle
import com.datn.apptravel.databinding.ActivitySplashBinding
import com.datn.apptravel.ui.base.BaseActivity
import com.datn.apptravel.ui.app.SplashViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashActivity : BaseActivity<ActivitySplashBinding, SplashViewModel>() {

    override val viewModel: SplashViewModel by viewModel()

    override fun getViewBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Initialize splash screen with delay
        viewModel.initSplash(SPLASH_DELAY)

        // Observe navigation events
        observeNavigation()
    }

    private fun observeNavigation() {
        viewModel.navigateToNext.observe(this) { navigationState ->
            when (navigationState) {

                SplashViewModel.SplashNavigationState.ToOnboarding -> {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                    finish()
                }
                SplashViewModel.SplashNavigationState.ToMain -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                SplashViewModel.SplashNavigationState.ToSignIn -> {
                    startActivity(Intent(this, com.datn.apptravel.ui.auth.SignInActivity::class.java))
                    finish()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Disable back button on splash screen
        // Intentionally not calling super to prevent going back
    }

    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }
}
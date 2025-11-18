package com.datn.apptravel.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.datn.apptravel.R
import com.datn.apptravel.ui.base.BaseViewModel
import com.datn.apptravel.ui.common.model.OnboardingData

class OnboardingViewModel : BaseViewModel() {

    private val _onboardingData = MutableLiveData<List<OnboardingData>>()
    val onboardingData: LiveData<List<OnboardingData>> = _onboardingData

    private val _currentPosition = MutableLiveData(0)
    val currentPosition: LiveData<Int> = _currentPosition

    init {
        loadOnboardingData()
    }

    private fun loadOnboardingData() {
        val commonTitle = "Welcome\nto Trip Planner"
        val commonDesc = "A perfect travel companion that simplifies trip planning and helps you explore incredible places around the world"
        val data = listOf(
            OnboardingData(
                imageRes = R.drawable.onboarding_1,
                title = commonTitle,
                description = commonDesc,
                showBackButton = false,
                showNextButton = true,
                showStartButton = false
            ),
            OnboardingData(
                imageRes = R.drawable.onboarding_2,
                title = "Get started",
                description = commonDesc,
                showBackButton = true,
                showNextButton = false,
                showStartButton = true
            )
        )

        _onboardingData.value = data
    }

    fun navigateToNextPage() {
        val currentPos = _currentPosition.value ?: 0
        val maxPosition = _onboardingData.value?.size?.minus(1) ?: 0

        if (currentPos < maxPosition) {
            _currentPosition.value = currentPos + 1
        }
    }

    fun navigateToPreviousPage() {
        val currentPos = _currentPosition.value ?: 0

        if (currentPos > 0) {
            _currentPosition.value = currentPos - 1
        }
    }

    fun updatePosition(position: Int) {
        _currentPosition.value = position
    }
}
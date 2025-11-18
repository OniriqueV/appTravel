package com.datn.apptravel.ui.common.model

data class OnboardingData(
    val imageRes: Int,
    val title: String,
    val description: String,
    val showBackButton: Boolean,
    val showNextButton: Boolean,
    val showStartButton: Boolean
)

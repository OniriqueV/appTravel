package com.datn.apptravel.ui.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,

) : BaseViewModel() {
    
    private val _navigateToNext = MutableLiveData<SplashNavigationState>()
    val navigateToNext: LiveData<SplashNavigationState> = _navigateToNext

    fun initSplash(splashDelay: Long = 2000) {
        viewModelScope.launch {
            setLoading(true)
            delay(splashDelay)
            
            try {
                
                // Check if user is logged in
                val isLoggedIn = authRepository.isLoggedIn()
                
                _navigateToNext.value = if (isLoggedIn) {
                    SplashNavigationState.ToMain
                } else {
                    SplashNavigationState.ToOnboarding
                }
            } catch (e: Exception) {
                setError("Error checking login status: ${e.message}")
                _navigateToNext.value = SplashNavigationState.ToSignIn
            } finally {
                setLoading(false)
            }
        }
    }

    sealed class SplashNavigationState {
        data object ToSignIn : SplashNavigationState()
        data object ToOnboarding : SplashNavigationState()
        data object ToMain : SplashNavigationState()
    }
}
package com.datn.apptravel.ui.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class MainViewModel(private val authRepository: AuthRepository) : BaseViewModel() {
    
    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> = _isUserLoggedIn
    
    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = authRepository.isLoggedIn()
                _isUserLoggedIn.value = isLoggedIn
            } catch (e: Exception) {
                setError("Error checking login status: ${e.message}")
                _isUserLoggedIn.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            setLoading(true)
            try {
                authRepository.logout()
                _isUserLoggedIn.value = false
            } catch (e: Exception) {
                setError("Error logging out: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }
}
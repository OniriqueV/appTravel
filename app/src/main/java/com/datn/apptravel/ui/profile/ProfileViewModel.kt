package com.datn.apptravel.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class ProfileViewModel(private val authRepository: AuthRepository) : BaseViewModel() {

    private val _logoutResult = MutableLiveData<Boolean>()
    val logoutResult: LiveData<Boolean> = _logoutResult

    fun getUserProfile() {
        // TODO: Implement with repository
        setLoading(false)
    }

    fun logout() {
        viewModelScope.launch {
            setLoading(true)
            try {
                authRepository.logout()
                _logoutResult.value = true
            } catch (e: Exception) {
                setError("Error logging out: ${e.message}")
                _logoutResult.value = false
            } finally {
                setLoading(false)
            }
        }
    }
}
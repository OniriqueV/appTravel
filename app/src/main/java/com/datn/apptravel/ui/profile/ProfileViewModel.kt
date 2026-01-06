package com.datn.apptravels.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravels.data.model.User
import com.datn.apptravels.data.repository.AuthRepository
import com.datn.apptravels.data.repository.UserRepository
import com.datn.apptravels.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _logoutResult = MutableLiveData<Boolean>()
    val logoutResult: LiveData<Boolean> = _logoutResult

    fun getUserProfile() {
        viewModelScope.launch {
            setLoading(true)
            try {
                val currentUser = authRepository.getCurrentFirebaseUser()
                if (currentUser != null) {
                    val user = userRepository.getUserById(currentUser.uid)
                    _userProfile.value = user
                } else {
                    setError("Không tìm thấy thông tin người dùng")
                }
            } catch (e: Exception) {
                setError("Lỗi khi tải thông tin: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            setLoading(true)
            try {
                authRepository.logout()
                _logoutResult.value = true
            } catch (e: Exception) {
                setError("Lỗi khi đăng xuất: ${e.message}")
                _logoutResult.value = false
            } finally {
                setLoading(false)
            }
        }
    }
}
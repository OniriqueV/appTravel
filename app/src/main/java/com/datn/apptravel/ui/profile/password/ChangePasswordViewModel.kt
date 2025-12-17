package com.datn.apptravel.ui.profile.password

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _changePasswordResult = MutableLiveData<Boolean>()
    val changePasswordResult: LiveData<Boolean> = _changePasswordResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null && user.email != null) {
                    // Re-authenticate user
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                    user.reauthenticate(credential).await()

                    // Update password
                    user.updatePassword(newPassword).await()

                    _changePasswordResult.value = true
                } else {
                    _error.value = "Không tìm thấy thông tin người dùng"
                    _changePasswordResult.value = false
                }
            } catch (e: Exception) {
                when {
                    e.message?.contains("password is invalid") == true -> {
                        _error.value = "Mật khẩu hiện tại không đúng"
                    }
                    e.message?.contains("network") == true -> {
                        _error.value = "Lỗi kết nối mạng"
                    }
                    else -> {
                        _error.value = "Lỗi khi đổi mật khẩu: ${e.message}"
                    }
                }
                _changePasswordResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
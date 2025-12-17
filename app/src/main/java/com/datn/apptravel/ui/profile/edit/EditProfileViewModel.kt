package com.datn.apptravel.ui.profile.edit

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.model.User
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.data.repository.UserRepository
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun getUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentFirebaseUser()
                if (currentUser != null) {
                    val user = userRepository.getUserById(currentUser.uid)
                    _userProfile.value = user
                } else {
                    _error.value = "Không tìm thấy thông tin người dùng"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi khi tải thông tin: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentFirebaseUser()
                if (currentUser != null) {
                    val updates = mutableMapOf<String, Any>(
                        "firstName" to firstName,
                        "lastName" to lastName
                    )

                    // Convert ảnh sang Base64 nếu có
                    if (imageUri != null) {
                        try {
                            val base64Image = userRepository.convertImageToBase64(imageUri)
                            updates["profilePicture"] = base64Image
                        } catch (e: Exception) {
                            _error.value = "Lỗi khi xử lý ảnh: ${e.message}"
                            _updateResult.value = false
                            _isLoading.value = false
                            return@launch
                        }
                    }

                    // Cập nhật thông tin user
                    userRepository.updateUser(currentUser.uid, updates)
                    _updateResult.value = true
                } else {
                    _error.value = "Không tìm thấy thông tin người dùng"
                    _updateResult.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Lỗi khi cập nhật: ${e.message}"
                _updateResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
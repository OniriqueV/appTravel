package com.datn.apptravel.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.model.User
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    // Sign in result
    private val _signInResult = MutableLiveData<Result<User>>()
    val signInResult: LiveData<Result<User>> = _signInResult

    // Sign up result
    private val _signUpResult = MutableLiveData<Result<User>>()
    val signUpResult: LiveData<Result<User>> = _signUpResult

    // Google sign in result
    private val _googleSignInResult = MutableLiveData<Result<User>>()
    val googleSignInResult: LiveData<Result<User>> = _googleSignInResult

    // Reset password result
    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val result = authRepository.login(email, password)
                _signInResult.value = result

                if (result.isFailure) {
                    setError(result.exceptionOrNull()?.message ?: "Sign in failed")
                }
            } catch (e: Exception) {
                _signInResult.value = Result.failure(e)
                setError(e.message ?: "Sign in failed")
            } finally {
                setLoading(false)
            }
        }
    }
    fun signUp(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val result = authRepository.signUp(email, password, firstName, lastName)
                _signUpResult.value = result

                if (result.isFailure) {
                    setError(result.exceptionOrNull()?.message ?: "Sign up failed")
                }
            } catch (e: Exception) {
                _signUpResult.value = Result.failure(e)
                setError(e.message ?: "Sign up failed")
            } finally {
                setLoading(false)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val result = authRepository.signInWithGoogle(idToken)
                _googleSignInResult.value = result

                if (result.isFailure) {
                    setError(result.exceptionOrNull()?.message ?: "Google sign in failed")
                }
            } catch (e: Exception) {
                _googleSignInResult.value = Result.failure(e)
                setError(e.message ?: "Google sign in failed")
            } finally {
                setLoading(false)
            }
        }
    }


    fun resetPassword(email: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val result = authRepository.forgotPassword(email)
                _resetPasswordResult.value = result

                if (result.isFailure) {
                    setError(result.exceptionOrNull()?.message ?: "Failed to send reset email")
                }
            } catch (e: Exception) {
                _resetPasswordResult.value = Result.failure(e)
                setError(e.message ?: "Failed to send reset email")
            } finally {
                setLoading(false)
            }
        }
    }
}
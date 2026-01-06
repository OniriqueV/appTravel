package com.datn.apptravels.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error handling
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    protected fun setError(message: String?) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }
}
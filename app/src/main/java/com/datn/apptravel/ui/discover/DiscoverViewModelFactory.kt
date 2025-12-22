package com.datn.apptravel.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.ui.discover.network.DiscoverRepository

class DiscoverViewModelFactory(
    private val repository: DiscoverRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoverViewModel( repository,
                 sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

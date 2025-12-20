package com.datn.apptravel.ui.discover.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.datn.apptravel.ui.discover.network.DiscoverRepository

class PostDetailViewModelFactory(
    private val repository: DiscoverRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostDetailViewModel(repository) as T
        }
        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}

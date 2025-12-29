package com.datn.apptravel.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: SearchRepository
) : ViewModel() {

    private val _items = MutableLiveData<List<SearchItem>>()
    val items: LiveData<List<SearchItem>> = _items

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _items.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val res = repository.search(keyword)
                val uiItems = mutableListOf<SearchItem>()

                if (res.users.isNotEmpty()) {
                    uiItems.add(SearchItem.Header("Users"))
                    res.users.forEach {
                        uiItems.add(
                            SearchItem.UserItem(
                                userId = it.id,
                                name = it.fullName,
                                avatar = it.profilePicture
                            )
                        )
                    }
                }

                if (res.trips.isNotEmpty()) {
                    uiItems.add(SearchItem.Header("Trips"))
                    res.trips.forEach {
                        uiItems.add(
                            SearchItem.TripItem(
                                tripId = it.id,
                                title = it.title,
                                image = it.coverPhoto,
                                tags = it.tags
                            )
                        )
                    }
                }

                _items.value = uiItems
            } catch (e: Exception) {
                _items.value = emptyList()
            }
        }
    }
}

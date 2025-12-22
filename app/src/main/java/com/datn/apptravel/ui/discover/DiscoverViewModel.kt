package com.datn.apptravel.ui.discover

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.ui.discover.model.DiscoverItem
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val repository: DiscoverRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val currentUserId: String?
        get() = sessionManager.getUserId()

    companion object {
        private const val PAGE_SIZE = 10
    }

    val discoverList = MutableLiveData<List<DiscoverItem>>()
    val followingList = MutableLiveData<List<DiscoverItem>>()
    val errorMessage = MutableLiveData<String?>()

    // ================= EXPLORE =================
    fun loadDiscover(page: Int = 0) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                discoverList.postValue(
                    repository.getDiscover(userId, page, PAGE_SIZE)
                )
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách khám phá")
            }
        }
    }

    // ================= FOLLOWING =================
    fun loadFollowing(page: Int = 0) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                followingList.postValue(
                    repository.getFollowing(userId, page, PAGE_SIZE)
                )
            } catch (e: Exception) {
                errorMessage.postValue("Không tải được danh sách Following")
            }
        }
    }

    // 🔥 Feed chỉ follow 1 chiều → chỉ update state local cho UI
    fun updateFollowState(userId: String, isFollowing: Boolean) {
        discoverList.value?.let { list ->
            discoverList.postValue(
                list.map {
                    if (it.userId == userId) it.copy(isFollowing = isFollowing) else it
                }
            )
        }
    }

    fun forceReload() {
        loadDiscover()
        loadFollowing()
    }
}

package com.datn.apptravels.ui.notification

import androidx.lifecycle.viewModelScope
import com.datn.apptravels.data.model.Notification
import com.datn.apptravels.data.repository.NotificationRepository
import com.datn.apptravels.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : BaseViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _allNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val allNotifications: StateFlow<List<Notification>> = _allNotifications.asStateFlow()
    
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    companion object {
        const val ITEMS_PER_PAGE = 6
    }

    init {
        loadNotificationSettings()
    }

    fun getNotifications() {
        setLoading(true)
        viewModelScope.launch {
            try {
                android.util.Log.d("NotificationViewModel", "Fetching notifications from API...")
                val result = notificationRepository.getNotifications()
                result.onSuccess { notifications ->
                    android.util.Log.d("NotificationViewModel", "✅ Got ${notifications.size} notifications from API")
                    _allNotifications.value = notifications
                    updatePagination()
                }.onFailure { error ->
                    android.util.Log.e("NotificationViewModel", "❌ Failed to get notifications: ${error.message}", error)
                    _allNotifications.value = emptyList()
                    _notifications.value = emptyList()
                    _totalPages.value = 0
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationViewModel", "❌ Exception in getNotifications: ${e.message}", e)
                _allNotifications.value = emptyList()
                _notifications.value = emptyList()
                _totalPages.value = 0
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updatePagination() {
        try {
            val allNotifs = _allNotifications.value
            val totalPages = (allNotifs.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            _totalPages.value = totalPages.coerceAtLeast(1)
            
            android.util.Log.d("NotificationViewModel", "updatePagination: ${allNotifs.size} notifs → $totalPages pages")
            
            // Ensure current page is valid
            if (_currentPage.value >= totalPages && totalPages > 0) {
                _currentPage.value = totalPages - 1
            }
            
            updateCurrentPageNotifications()
        } catch (e: Exception) {
            android.util.Log.e("NotificationViewModel", "❌ Error in updatePagination: ${e.message}", e)
        }
    }

    private fun updateCurrentPageNotifications() {
        val allNotifs = _allNotifications.value
        val startIndex = _currentPage.value * ITEMS_PER_PAGE
        val endIndex = (startIndex + ITEMS_PER_PAGE).coerceAtMost(allNotifs.size)
        
        _notifications.value = if (startIndex < allNotifs.size) {
            allNotifs.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    fun setPage(page: Int) {
        if (page in 0 until _totalPages.value) {
            _currentPage.value = page
            updateCurrentPageNotifications()
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            // Update all notifications list
            val updatedAllList = _allNotifications.value.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            _allNotifications.value = updatedAllList
            updateCurrentPageNotifications()

            // Update backend
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val unreadNotifications = _allNotifications.value.filter { !it.isRead }
            
            // Update UI immediately
            val updatedList = _allNotifications.value.map { it.copy(isRead = true) }
            _allNotifications.value = updatedList
            updateCurrentPageNotifications()

            // Update backend for each unread notification
            unreadNotifications.forEach { notification ->
                notificationRepository.markAsRead(notification.id)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            // Update UI immediately
            val updatedList = _allNotifications.value.filter { it.id != notificationId }
            _allNotifications.value = updatedList
            updatePagination()

            // Delete from backend
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        viewModelScope.launch {
            notificationRepository.saveNotificationSettings(enabled)
        }
    }

    fun getUnreadCount(): Int {
        return _allNotifications.value.count { !it.isRead }
    }

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            // Load from cache (instant, no API call)
            val result = notificationRepository.getNotificationSettings()
            result.onSuccess { enabled ->
                _notificationsEnabled.value = enabled
                android.util.Log.d("NotificationViewModel", "📱 Loaded notification settings from cache: $enabled")
            }
        }
    }


}
package com.datn.apptravels.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.data.model.Notification
import com.datn.apptravels.databinding.PageNotificationsBinding

class NotificationPagerAdapter(
    private val onItemClick: (Notification) -> Unit,
    private val onItemDelete: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationPagerAdapter.PageViewHolder>() {

    private var allNotifications: List<Notification> = emptyList()
    private val itemsPerPage = 6

    fun submitList(notifications: List<Notification>) {
        android.util.Log.d("NotificationPagerAdapter", "submitList: ${notifications.size} notifications")
        allNotifications = notifications
        notifyDataSetChanged()
        android.util.Log.d("NotificationPagerAdapter", "Item count: $itemCount pages")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        android.util.Log.d("NotificationPagerAdapter", "onCreateViewHolder called")
        val binding = PageNotificationsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding, onItemClick, onItemDelete)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        android.util.Log.d("NotificationPagerAdapter", "onBindViewHolder: page $position")
        try {
            val startIndex = position * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(allNotifications.size)
            val pageNotifications = allNotifications.subList(startIndex, endIndex)
            android.util.Log.d("NotificationPagerAdapter", "  Binding ${pageNotifications.size} notifications (index $startIndex-$endIndex)")
            holder.bind(pageNotifications)
        } catch (e: Exception) {
            android.util.Log.e("NotificationPagerAdapter", "❌ Error binding page $position: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int {
        return (allNotifications.size + itemsPerPage - 1) / itemsPerPage
    }

    class PageViewHolder(
        private val binding: PageNotificationsBinding,
        private val onItemClick: (Notification) -> Unit,
        private val onItemDelete: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val adapter = NotificationAdapter(onItemClick)

        init {
            binding.rvPageNotifications.adapter = adapter
            
            // Setup swipe-to-delete
            val swipeHandler = NotificationSwipeHandler(
                onDelete = { position ->
                    adapter.currentList.getOrNull(position)?.let { notification ->
                        onItemDelete(notification)
                    }
                }
            )
            swipeHandler.attachToRecyclerView(binding.rvPageNotifications)
        }

        fun bind(notifications: List<Notification>) {
            adapter.submitList(notifications)
        }
    }
}

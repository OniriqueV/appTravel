package com.datn.apptravel.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.R
import com.datn.apptravel.data.model.Notification
import com.datn.apptravel.data.model.NotificationType
import com.datn.apptravel.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onItemClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvTimeAgo.text = getTimeAgo(notification.timestamp)

                // Set icon based on notification type
                when (notification.type) {
                    NotificationType.FLIGHT -> {
                        iconNotification.setImageResource(R.drawable.ic_flight)
                        iconNotification.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.blue_primary)
                        )
                    }
                    NotificationType.ACTIVITY -> {
                        iconNotification.setImageResource(R.drawable.ic_camera)
                        iconNotification.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.blue_primary)
                        )
                    }
                    NotificationType.TRIP -> {
                        iconNotification.setImageResource(R.drawable.ic_luggage)
                        iconNotification.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.blue_primary)
                        )
                    }
                    else -> {
                        iconNotification.setImageResource(R.drawable.ic_flight)
                        iconNotification.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.grey_600)
                        )
                    }
                }

                // Show unread indicator
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

                root.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                minutes < 1 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days < 7 -> "$days ngày trước"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}

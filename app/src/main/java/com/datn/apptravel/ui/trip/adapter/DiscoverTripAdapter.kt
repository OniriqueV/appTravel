package com.datn.apptravels.ui.trip.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravels.R
import com.datn.apptravels.data.repository.TripRepository
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.ui.discover.model.DiscoverItem
import com.datn.apptravels.utils.ApiConfig
import de.hdodenhof.circleimageview.CircleImageView

class DiscoverTripAdapter(
    private var originalItems: List<DiscoverItem>,
    private val tripRepository: TripRepository,
    private val sessionManager: SessionManager,
    private val onItemClick: (DiscoverItem) -> Unit
) : RecyclerView.Adapter<DiscoverTripAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var currentPosition = 0
    private var isUserScrolling = false
    private var isAutoScrolling = false
    private val snapHelper = LinearSnapHelper()
    
    // Create infinite list by repeating items
    private val REPEAT_COUNT = 1000 // Số lần lặp để tạo hiệu ứng vô hạn
    private var discoverItems: List<DiscoverItem> = createInfiniteList(originalItems)
    
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    isUserScrolling = true
                    isAutoScrolling = false
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                }
                RecyclerView.SCROLL_STATE_IDLE -> {
                    if (isUserScrolling) {
                        // User just stopped scrolling
                        isUserScrolling = false
                        // Update currentPosition based on visible item
                        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
                        layoutManager?.let {
                            val firstVisiblePosition = it.findFirstCompletelyVisibleItemPosition()
                            if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                                currentPosition = firstVisiblePosition
                            }
                        }
                        // Resume auto scroll after user stops
                        autoScrollHandler.postDelayed(autoScrollRunnable, 5000)
                    } else if (isAutoScrolling) {
                        // Auto scroll just finished
                        isAutoScrolling = false
                        // Schedule next auto scroll
                        autoScrollHandler.postDelayed(autoScrollRunnable, 5000)
                    }
                }
            }
        }
    }
    
    private fun createInfiniteList(items: List<DiscoverItem>): List<DiscoverItem> {
        if (items.isEmpty()) return emptyList()
        val infiniteList = mutableListOf<DiscoverItem>()
        repeat(REPEAT_COUNT) {
            infiniteList.addAll(items)
        }
        return infiniteList
    }

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (discoverItems.isNotEmpty() && recyclerView != null && !isUserScrolling && !isAutoScrolling) {
                isAutoScrolling = true
                val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
                layoutManager?.let {
                    // Get current first visible position
                    val firstVisiblePosition = it.findFirstVisibleItemPosition()
                    val view = it.findViewByPosition(firstVisiblePosition)
                    view?.let { v ->
                        // Calculate scroll distance (item width + margin)
                        val scrollDistance = v.width
                        recyclerView?.smoothScrollBy(scrollDistance, 0)
                        currentPosition = firstVisiblePosition + 1
                    }
                }
                // Note: Next scroll will be scheduled when scroll completes (in SCROLL_STATE_IDLE)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        
        // Attach snap helper for smooth item snapping
        try {
            snapHelper.attachToRecyclerView(recyclerView)
        } catch (e: IllegalStateException) {
            // SnapHelper already attached, ignore
        }
        
        recyclerView.addOnScrollListener(scrollListener)
        if (discoverItems.isNotEmpty()) {
            // Scroll to middle position to allow infinite scrolling
            val middlePosition = discoverItems.size / 2
            recyclerView.scrollToPosition(middlePosition)
            currentPosition = middlePosition
            autoScrollHandler.postDelayed(autoScrollRunnable, 5000)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnScrollListener(scrollListener)
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        this.recyclerView = null
    }

    fun updateItems(newItems: List<DiscoverItem>) {
        originalItems = newItems
        discoverItems = createInfiniteList(newItems)
        notifyDataSetChanged()
        
        // Restart auto-scroll from middle position
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        if (discoverItems.isNotEmpty()) {
            val middlePosition = discoverItems.size / 2
            recyclerView?.scrollToPosition(middlePosition)
            currentPosition = middlePosition
            autoScrollHandler.postDelayed(autoScrollRunnable, 5000)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_tripui, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(discoverItems[position])
    }

    override fun getItemCount(): Int = discoverItems.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivTripImage: ImageView = itemView.findViewById(R.id.ivDiscoverTripImage)
        private val ivUserAvatar: CircleImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTripTitle: TextView = itemView.findViewById(R.id.tvDiscoverTripTitle)
        private val tvTripStartDate: TextView = itemView.findViewById(R.id.tvDiscoverTripStartDate)
        private val tvTripDuration: TextView = itemView.findViewById(R.id.tvDiscoverTripDuration)

        fun bind(item: DiscoverItem) {
            android.util.Log.d("DiscoverTripAdapter", "TripId: ${item.tripId}")
            
            // Try to get cached data first
            val cachedDetail = sessionManager.getCachedDiscoverTripDetail(item.tripId)
            
            if (cachedDetail != null) {
                // Use cached data - NO API CALL!
                android.util.Log.d("DiscoverTripAdapter", "Using cached data for trip ${item.tripId}")
                
                // Set trip content (user's feelings/review) for Discover tab
                tvTripTitle.text = cachedDetail.trip.content?.ifEmpty { 
                    cachedDetail.trip.title.ifEmpty { item.caption ?: "Explore Adventure" }
                } ?: cachedDetail.trip.title.ifEmpty { item.caption ?: "Explore Adventure" }
                
                // Set user name
                val fullName = "${cachedDetail.user.firstName} ${cachedDetail.user.lastName}".trim()
                tvUserName.text = fullName.ifEmpty { "User" }
                
                // Set start date and duration separately
                tvTripStartDate.text = cachedDetail.startDateText
                tvTripDuration.text = cachedDetail.durationText
                
                // Set trip image
                if (!item.tripImage.isNullOrEmpty()) {
                    val imageUrl = ApiConfig.getImageUrl(item.tripImage)
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_a)
                        .error(R.drawable.bg_a)
                        .centerCrop()
                        .into(ivTripImage)
                } else {
                    ivTripImage.setImageResource(R.drawable.bg_a)
                }
                
                // Load user avatar
                if (!cachedDetail.user.profilePicture.isNullOrEmpty()) {
                    val avatarUrl = when {
                        cachedDetail.user.profilePicture.startsWith("http") -> cachedDetail.user.profilePicture
                        cachedDetail.user.profilePicture.startsWith("data:") -> cachedDetail.user.profilePicture
                        else -> ApiConfig.getImageUrl(cachedDetail.user.profilePicture)
                    }
                    
                    Glide.with(itemView.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivUserAvatar)
                } else if (!item.userAvatar.isNullOrEmpty()) {
                    // Fallback to item avatar
                    val avatarUrl = when {
                        item.userAvatar.startsWith("http") -> item.userAvatar
                        item.userAvatar.startsWith("data:") -> item.userAvatar
                        else -> ApiConfig.getImageUrl(item.userAvatar)
                    }
                    
                    Glide.with(itemView.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivUserAvatar)
                } else {
                    ivUserAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                // Fallback: use item data (should rarely happen)
                android.util.Log.w("DiscoverTripAdapter", "No cached data for trip ${item.tripId}, using item data")
                
                tvTripTitle.text = item.caption ?: "Explore Adventure"
                tvUserName.text = if (item.userName.isNotEmpty()) item.userName else "User"
                tvTripStartDate.text = "Loading..."
                tvTripDuration.text = ""
                
                // Set trip image
                if (!item.tripImage.isNullOrEmpty()) {
                    val imageUrl = ApiConfig.getImageUrl(item.tripImage)
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_a)
                        .error(R.drawable.bg_a)
                        .centerCrop()
                        .into(ivTripImage)
                } else {
                    ivTripImage.setImageResource(R.drawable.bg_a)
                }
                
                // Load avatar from item
                if (!item.userAvatar.isNullOrEmpty()) {
                    val avatarUrl = when {
                        item.userAvatar.startsWith("http") -> item.userAvatar
                        item.userAvatar.startsWith("data:") -> item.userAvatar
                        else -> ApiConfig.getImageUrl(item.userAvatar)
                    }
                    
                    Glide.with(itemView.context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivUserAvatar)
                } else {
                    ivUserAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

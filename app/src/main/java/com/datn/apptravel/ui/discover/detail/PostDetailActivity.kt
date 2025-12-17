package com.datn.apptravel.ui.discover.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.model.PostDetailResponse
import com.datn.apptravel.ui.discover.network.DiscoverApiClient
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravel.ui.discover.adapter.CommentAdapter
import com.datn.apptravel.ui.discover.model.CreatePostCommentRequest


class PostDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: DiscoverViewModel

    private lateinit var imgUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var imgPostImage: ImageView
    private lateinit var tvPostContent: TextView

    private lateinit var cardTrip: View
    private lateinit var imgTripCover: ImageView
    private lateinit var tvTripTitle: TextView

    private lateinit var progressBar: ProgressBar

    private var postId: String = ""
    private var userId: String? = null

    private lateinit var btnLike: ImageView
    private lateinit var tvLikeCount: TextView

    private var isLiked = false
    private var likeCount = 0

    private lateinit var edtComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var recyclerComments: RecyclerView
    private lateinit var commentAdapter: CommentAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        postId = intent.getStringExtra("postId") ?: ""
        userId = intent.getStringExtra("userId")

        viewModel = ViewModelProvider(this)[DiscoverViewModel::class.java]

        initViews()
        observeViewModel()
        if (postId.isNotEmpty()) {
            progressBar.visibility = View.VISIBLE
            viewModel.getPostDetail(postId, userId)
            loadComments()
        }


        if (postId.isNotEmpty()) {
            progressBar.visibility = View.VISIBLE
            viewModel.getPostDetail(postId, userId)
        }

    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        imgUserAvatar = findViewById(R.id.imgUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        imgPostImage = findViewById(R.id.imgPostImage)
        tvPostContent = findViewById(R.id.tvPostContent)

        cardTrip = findViewById(R.id.cardTrip)
        imgTripCover = findViewById(R.id.imgTripCover)
        tvTripTitle = findViewById(R.id.tvTripTitle)

        progressBar = findViewById(R.id.progressPostDetail)

        btnLike = findViewById(R.id.btnLike)
        tvLikeCount = findViewById(R.id.tvLikeCount)

        edtComment = findViewById(R.id.edtComment)
        btnSendComment = findViewById(R.id.btnSendComment)
        recyclerComments = findViewById(R.id.recyclerComments)

        commentAdapter = CommentAdapter()
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.adapter = commentAdapter

        btnLike.setOnClickListener {
            if (isLiked) {
                unlikePost()
            } else {
                likePost()
            }
        }

        btnSendComment.setOnClickListener {
            sendComment()
        }
    }

    private fun observeViewModel() {
        viewModel.postDetail.observe(this) { detail ->
            progressBar.visibility = View.GONE
            detail?.let { bindPostDetail(it) }
        }

        viewModel.errorMessage.observe(this) {
            if (!it.isNullOrEmpty()) {
                progressBar.visibility = View.GONE
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindPostDetail(detail: PostDetailResponse) {
        tvUserName.text = detail.user.userName
        tvPostContent.text = detail.post.content
        tvCreatedAt.text = formatTime(detail.post.createdAt)

        likeCount = detail.likes.count
        isLiked = detail.likes.userLiked

        tvLikeCount.text = likeCount.toString()

        Glide.with(this)
            .load(detail.user.avatar)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .into(imgUserAvatar)

        detail.post.images.firstOrNull()?.let {
            Glide.with(this).load(it).into(imgPostImage)
        }



        handleTripCard(detail)
        updateLikeIcon()
    }

    private fun handleTripCard(detail: PostDetailResponse) {
        val trip = detail.trip
        if (trip != null) {
            cardTrip.visibility = View.VISIBLE
            tvTripTitle.text = trip.title

            Glide.with(this)
                .load(trip.coverPhoto)
                .into(imgTripCover)

            cardTrip.setOnClickListener {
                openTripDetail(trip.tripId)
            }
        } else {
            cardTrip.visibility = View.GONE
        }
    }

    private fun openTripDetail(tripId: String) {
        val intent = Intent().apply {
            setClassName(
                this@PostDetailActivity,
                "com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity"
            )
            putExtra("tripId", tripId)
        }
        startActivity(intent)
    }

    private fun formatTime(timestamp: Long): String {
        val minutes = (System.currentTimeMillis() - timestamp) / 60000
        return "$minutes phút trước"
    }

    private fun updateLikeIcon() {
        btnLike.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun likePost() {
        lifecycleScope.launch {
            try {
                val uid = userId ?: return@launch
                DiscoverApiClient.api.likePost(postId, uid)

                isLiked = true
                likeCount++

                tvLikeCount.text = likeCount.toString()
                updateLikeIcon()

            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Like thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unlikePost() {
        lifecycleScope.launch {
            try {
                val uid = userId ?: return@launch
                DiscoverApiClient.api.unlikePost(postId, uid)

                isLiked = false
                likeCount = maxOf(0, likeCount - 1)

                tvLikeCount.text = likeCount.toString()
                updateLikeIcon()

            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Unlike thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val comments =
                    DiscoverApiClient.api.getPostComments(postId)
                commentAdapter.submit(comments)
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity,
                    "Không load được comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendComment() {
        val text = edtComment.text.toString().trim()
        if (text.isBlank()) return

        val uid = userId ?: run {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }
        val req = CreatePostCommentRequest(
            userId = uid,
            userName = tvUserName.text.toString(),
            avatar = null,
            content = text
        )


        lifecycleScope.launch {
            try {
                DiscoverApiClient.api.addPostComment(postId, req)
                edtComment.setText("")
                loadComments() // refresh
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity,
                    "Comment thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }




}

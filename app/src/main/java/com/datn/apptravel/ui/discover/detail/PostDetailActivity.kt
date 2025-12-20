package com.datn.apptravel.ui.discover.detail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.adapter.CommentAdapter
import com.datn.apptravel.ui.discover.model.CommentUiModel
import com.datn.apptravel.ui.discover.model.CreatePostCommentRequest
import com.datn.apptravel.ui.discover.model.PostUiModel
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import com.datn.apptravel.ui.discover.post.ImageUrlUtil
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*


class PostDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "postId"
        const val EXTRA_OPEN_COMMENT = "openComment"

        const val RESULT_POST_ID = "postId"
        const val RESULT_COMMENT_COUNT = "commentCount"
    }

    // ================= DEPENDENCY =================
    private val repository = DiscoverRepository()

    // ================= UI =================
    private lateinit var btnBack: ImageButton
    private lateinit var imgUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvPostContent: TextView
    private lateinit var btnLike: ImageView
    private lateinit var tvLikeCount: TextView

    private lateinit var recyclerComments: RecyclerView
    private lateinit var edtComment: EditText
    private lateinit var btnSendComment: ImageView
    private lateinit var progress: View
    private lateinit var imgTripCover: ImageView
    private lateinit var cardTrip: View
    private lateinit var commentAdapter: CommentAdapter

    // ================= STATE =================
    private lateinit var postId: String
    private var openComment = false
    private var currentPost: PostUiModel? = null

    private val imageBaseUrl = "http://10.0.2.2:8080/api/files/"

    // ================= LIFECYCLE =================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        postId = intent.getStringExtra(EXTRA_POST_ID).orEmpty()
        openComment = intent.getBooleanExtra(EXTRA_OPEN_COMMENT, false)

        if (postId.isBlank()) {
            Toast.makeText(this, "Thiếu postId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()

        // 🔙 Back
        onBackPressedDispatcher.addCallback(this) { handleBack() }
        btnBack.setOnClickListener { handleBack() }

        setupComments()

        btnSendComment.setOnClickListener { sendComment() }
        btnLike.setOnClickListener { toggleLike() }

        loadDetail()
    }

    // ================= UI SETUP =================
    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        imgUserAvatar = findViewById(R.id.imgUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvPostContent = findViewById(R.id.tvPostContent)
        btnLike = findViewById(R.id.btnLike)
        tvLikeCount = findViewById(R.id.tvLikeCount)
        imgTripCover = findViewById(R.id.imgTripCover)
        cardTrip = findViewById(R.id.cardTrip)
        recyclerComments = findViewById(R.id.recyclerComments)
        edtComment = findViewById(R.id.edtComment)
        btnSendComment = findViewById(R.id.btnSendComment)
        progress = findViewById(R.id.progressPostDetail)
    }

    private fun setupComments() {
        commentAdapter = CommentAdapter()
        recyclerComments.layoutManager = LinearLayoutManager(this)
        recyclerComments.adapter = commentAdapter
    }

    // ================= LOAD DETAIL =================
    private fun loadDetail() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 1️⃣ Load post detail
                val detail = repository.getPostDetail(postId, currentUserId)
                renderPost(detail)
                val postOwnerId = detail.userId
                val postOwnerName = detail.userName
                // 2️⃣ Load comments
                val apiComments = repository.getPostComments(postId)

                val uiComments = apiComments.map { c ->
                    CommentUiModel(
                        id = c.commentId.orEmpty(),
                        userName = c.userName?.takeIf { it.isNotBlank() }
                            ?: c.userId?.let { uid ->
                                if (uid == currentUserId)
                                    FirebaseAuth.getInstance().currentUser?.displayName ?: "Bạn"
                                else
                                    "User"
                            } ?: "User",
                        userAvatar = c.userAvatarUrl,
                        content = c.content.orEmpty(),
                        createdAt = c.createdAt ?: 0L,
                        isMine = c.userId == currentUserId
                    )
                }

                commentAdapter.submitList(uiComments)

                if (openComment) focusCommentInput()

            } catch (e: Exception) {
                Toast.makeText(
                    this@PostDetailActivity,
                    "Load post detail thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    // ================= RENDER =================
    private fun renderPost(post: PostUiModel) {
        currentPost = post

        tvUserName.text = post.userName
        tvPostContent.text = post.caption ?: ""

        renderLike(post)

        // avatar
        Glide.with(this)
            .load(post.userAvatarUrl?.let { imageBaseUrl + it })
            .placeholder(R.drawable.ic_avatar_placeholder)
            .circleCrop()
            .into(imgUserAvatar)

        // 🔥 TRIP IMAGE (FIX MẤT ẢNH)
        if (!post.tripImage.isNullOrBlank()) {
            cardTrip.visibility = View.VISIBLE

            Glide.with(this)
                .load(ImageUrlUtil.toFullUrl(post.tripImage))
                .placeholder(R.drawable.bg_trip_placeholder)
                .error(R.drawable.bg_trip_placeholder)
                .into(imgTripCover)

            cardTrip.setOnClickListener {
                openTripDetail(post.tripId!!)
            }

        } else {
            cardTrip.visibility = View.GONE
        }
    }

    private fun renderLike(post: PostUiModel) {
        tvLikeCount.text = post.likeCount.toString()
        btnLike.setImageResource(
            if (post.isLiked)
                R.drawable.ic_heart_filled
            else
                R.drawable.ic_heart_outline
        )
    }

    // ================= ACTION =================
    private fun toggleLike() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val post = currentPost ?: return

        lifecycleScope.launch {
            try {
                if (post.isLiked) {
                    repository.unlikePost(post.postId, userId)
                } else {
                    repository.likePost(post.postId, userId)
                }

                // 🔥 LUÔN reload từ BE → state chuẩn tuyệt đối
                loadDetail()

            } catch (e: Exception) {
                Toast.makeText(
                    this@PostDetailActivity,
                    "Không thể cập nhật like",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendComment() {
        val content = edtComment.text.toString().trim()
        if (content.isBlank()) return

        val user = FirebaseAuth.getInstance().currentUser ?: return

        lifecycleScope.launch {
            try {
                repository.addPostComment(
                    postId,
                    CreatePostCommentRequest(
                        userId = user.uid,
                        userName = user.displayName ?: user.email ?: "User",
                        avatar = user.photoUrl?.toString(),
                        content = content
                    )
                )

                edtComment.setText("")
                loadDetail()

            } catch (e: Exception) {
                Toast.makeText(
                    this@PostDetailActivity,
                    "Gửi comment thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun focusCommentInput() {
        edtComment.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(edtComment, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun openTripDetail(tripId: String) {
        val intent = Intent(
            this,
            com.datn.apptravel.ui.trip.detail.tripdetail.TripDetailActivity::class.java
        )
        intent.putExtra(
            com.datn.apptravel.ui.trip.TripsFragment.EXTRA_TRIP_ID,
            tripId
        )
        intent.putExtra("READ_ONLY", true)
        startActivity(intent)
    }

    // ================= BACK =================
    private fun handleBack() {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(RESULT_POST_ID, postId)
            }
        )
        finish()
    }
}

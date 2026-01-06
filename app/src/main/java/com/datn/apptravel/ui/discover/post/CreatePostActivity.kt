package com.datn.apptravel.ui.discover.post

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.model.User
import com.datn.apptravel.ui.discover.model.ShareTripRequest
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import com.datn.apptravel.ui.discover.util.ImageUrlUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CreatePostActivity : AppCompatActivity() {

    private val discoverRepository: DiscoverRepository by inject()

    private lateinit var btnBack: ImageButton
    private lateinit var tvSelectedTrip: TextView
    private lateinit var btnSelectTrip: View
    private lateinit var imgTripCover: ImageView

    private lateinit var btnVisibility: View
    private lateinit var tvVisibility: TextView
    private lateinit var btnAddFollower: ImageButton   // ✅ BỔ SUNG

    private lateinit var edtContent: EditText
    private lateinit var chipGroupTopic: ChipGroup

    private lateinit var progress: ProgressBar
    private lateinit var btnPublish: MaterialButton

    private var selectedTripId: String? = null
    private var selectedTripTitle: String? = null
    private var selectedTripImage: String? = null

    private var selectedVisibility: String = VIS_PUBLIC

    // ✅ GIỮ – DÙNG CHO FOLLOWER CUSTOM
    private val selectedUsers = mutableListOf<String>()

    private val selectTripLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            selectedTripId =
                data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_ID)
            selectedTripTitle =
                data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_TITLE)
            selectedTripImage =
                data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_IMAGE)

            tvSelectedTrip.text =
                selectedTripTitle?.let { "Trip: $it" } ?: "Trip selected"

            Glide.with(this)
                .load(ImageUrlUtil.toFullUrl(selectedTripImage))
                .placeholder(R.drawable.bg_trip_placeholder)
                .error(R.drawable.bg_trip_placeholder)
                .into(imgTripCover)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_create_post)
        supportActionBar?.hide()

        val topBar = findViewById<View>(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, status.top, v.paddingRight, v.paddingBottom)
            insets
        }

        window.statusBarColor = Color.parseColor("#C78C4D")

        bindViews()
        setupVisibilityMenu()

        btnBack.setOnClickListener { finish() }

        btnSelectTrip.setOnClickListener {
            val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
            val i = Intent(this, SelectTripForPostActivity::class.java).apply {
                putExtra(SelectTripForPostActivity.EXTRA_USER_ID, userId)
            }
            selectTripLauncher.launch(i)
        }

        // ✅ CLICK ➕ CHỌN FOLLOWER
        btnAddFollower.setOnClickListener {
            val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()

            AddFollowerBottomSheet(
                userId = userId,
                onUserSelected = { userId ->
                    selectedUsers.add(userId)
                    Toast.makeText(
                        this,
                        "Đã chọn ${selectedUsers.size} người",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ).show(supportFragmentManager, "AddFollower")

        }


        btnPublish.setOnClickListener { submitPost() }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSelectedTrip = findViewById(R.id.tvSelectedTrip)
        btnSelectTrip = findViewById(R.id.btnSelectTrip)
        imgTripCover = findViewById(R.id.imgTripCover)

        btnVisibility = findViewById(R.id.btnVisibility)
        tvVisibility = findViewById(R.id.tvVisibility)
        btnAddFollower = findViewById(R.id.btnAddFollower) // ✅ BIND

        edtContent = findViewById(R.id.edtContent)
        chipGroupTopic = findViewById(R.id.chipGroupTopic)

        progress = findViewById(R.id.progressCreatePost)
        btnPublish = findViewById(R.id.btnPublish)

        tvVisibility.text = selectedVisibility
        btnAddFollower.isVisible = false
    }

    private fun setupVisibilityMenu() {
        btnVisibility.setOnClickListener {
            val menu = PopupMenu(this, btnVisibility)
            menu.menu.add(VIS_PRIVATE)
            menu.menu.add(VIS_PUBLIC)
            menu.menu.add(VIS_FOLLOWER)

            menu.setOnMenuItemClickListener { item ->
                selectedVisibility = item.title.toString()
                tvVisibility.text = selectedVisibility

                // ✅ CHỈ FOLLOWER MỚI HIỆN ➕
                if (selectedVisibility == VIS_FOLLOWER) {
                    btnAddFollower.isVisible = true
                } else {
                    btnAddFollower.isVisible = false
                    selectedUsers.clear()
                }
                true
            }
            menu.show()
        }
    }

    private fun getSelectedTags(): String {
        return chipGroupTopic.children
            .filterIsInstance<Chip>()
            .filter { it.isChecked }
            .map {
                val tag = it.tag?.toString()?.trim()
                val text = it.text?.toString()?.trim()
                (tag?.takeIf { s -> s.isNotBlank() } ?: text).orEmpty()
            }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(",")
    }

    private fun submitPost() {
        val content = edtContent.text.toString().trim()
        val tripId = selectedTripId ?: return

        val isPublicValue = when (selectedVisibility) {
            VIS_PUBLIC -> "public"
            VIS_FOLLOWER -> "follower"
            VIS_PRIVATE -> "private"
            else -> "public"
        }

        progress.isVisible = true
        btnPublish.isEnabled = false

        lifecycleScope.launch {
            try {
                val req = ShareTripRequest(
                    tripId = tripId,
                    content = content,
                    tags = getSelectedTags(),
                    isPublic = if (selectedUsers.isNotEmpty()) "follower" else isPublicValue,
                    sharedWithUsers = selectedUsers.toList()
                )

                val result = discoverRepository.shareTrip(req)

                if (result.isSuccess) {
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this@CreatePostActivity,
                        "Chia sẻ thất bại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                progress.isVisible = false
                btnPublish.isEnabled = true
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "userId"
        private const val VIS_PRIVATE = "Private"
        private const val VIS_PUBLIC = "Public"
        private const val VIS_FOLLOWER = "Follower"
    }
}
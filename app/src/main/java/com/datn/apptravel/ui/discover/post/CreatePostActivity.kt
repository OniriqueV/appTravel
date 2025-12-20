package com.datn.apptravel.ui.discover.post

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatePostActivity : AppCompatActivity() {

    private val viewModel: DiscoverViewModel by viewModel()

    private lateinit var btnBack: ImageButton
    private lateinit var tvSelectedTrip: TextView
    private lateinit var btnSelectTrip: View
    private lateinit var imgTripCover: ImageView

    private lateinit var btnVisibility: View
    private lateinit var tvVisibility: TextView

    private lateinit var edtContent: EditText
    private lateinit var chipGroupTopic: ChipGroup

    private lateinit var progress: ProgressBar
    private lateinit var btnPublish: MaterialButton

    private var selectedTripId: String? = null
    private var selectedTripTitle: String? = null
    private var selectedTripImage: String? = null

    private var selectedVisibility: String = VIS_PUBLIC

    private val selectTripLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            selectedTripId = data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_ID)
            selectedTripTitle = data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_TITLE)
            selectedTripImage = data.getStringExtra(SelectTripForPostActivity.EXTRA_TRIP_IMAGE)

            tvSelectedTrip.text = selectedTripTitle?.let { "Trip: $it" } ?: "Trip selected"

            // ảnh post = ảnh trip (đúng logic bạn yêu cầu)
            val displayUrl = ImageUrlUtil.toFullUrl(selectedTripImage)
            Glide.with(this)
                .load(displayUrl)
                .placeholder(R.drawable.bg_trip_placeholder)
                .error(R.drawable.bg_trip_placeholder)
                .into(imgTripCover)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_create_post)
        viewModel.clearPostCreated()
        supportActionBar?.hide()
        val topBar = findViewById<View>(R.id.topBar)

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, status.top, v.paddingRight, v.paddingBottom)
            insets
        }

        window.statusBarColor = Color.parseColor("#C78C4D")

        bindViews()
        observeVM()
        setupVisibilityMenu()

        btnBack.setOnClickListener { finish() }

        btnSelectTrip.setOnClickListener {
            val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
            val i = Intent(this, SelectTripForPostActivity::class.java).apply {
                putExtra(SelectTripForPostActivity.EXTRA_USER_ID, userId)
            }
            selectTripLauncher.launch(i)
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

        edtContent = findViewById(R.id.edtContent)
        chipGroupTopic = findViewById(R.id.chipGroupTopic)

        progress = findViewById(R.id.progressCreatePost)
        btnPublish = findViewById(R.id.btnPublish)

        tvVisibility.text = selectedVisibility
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
                true
            }
            menu.show()
        }
    }

    private fun observeVM() {
        viewModel.isPosting.observe(this) { loading ->
            progress.isVisible = loading
            btnPublish.isEnabled = !loading
            btnSelectTrip.isEnabled = !loading
            btnVisibility.isEnabled = !loading
        }

        viewModel.postCreated.observe(this) { created ->
            if (created == true) {
                Toast.makeText(this, "Post published successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /** Tags lấy từ ChipGroup: chip nào checked thì lấy text/tag của chip đó */
    private fun getSelectedTags(): List<String> {
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
            .toList()
    }

    private fun submitPost() {
        val userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        if (userId.isBlank()) {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_SHORT).show()
            return
        }

        val content = edtContent.text.toString().trim()
        if (content.isBlank()) {
            Toast.makeText(this, "Content cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val tripId = selectedTripId
        if (tripId.isNullOrBlank()) {
            Toast.makeText(this, "Please select a trip", Toast.LENGTH_SHORT).show()
            return
        }

        val isPublic = selectedVisibility == VIS_PUBLIC
        val tags = getSelectedTags()

        val req = CreatePostRequest(
            userId = userId,
            tripId = tripId,
            content = content,
            isPublic = isPublic,
            tags = tags
        )

        viewModel.createPost(req)
    }

    companion object {
        const val EXTRA_USER_ID = "userId"

        private const val VIS_PRIVATE = "Private"
        private const val VIS_PUBLIC = "Public"
        private const val VIS_FOLLOWER = "Follower"
    }
}

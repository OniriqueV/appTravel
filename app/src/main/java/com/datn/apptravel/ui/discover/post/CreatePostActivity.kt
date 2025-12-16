package com.datn.apptravel.ui.discover.post

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.datn.apptravel.R
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.model.CreatePostRequest
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatePostActivity : AppCompatActivity() {

    private val viewModel: DiscoverViewModel by viewModel()

    private lateinit var edtTitle: EditText
    private lateinit var edtContent: EditText
    private lateinit var edtTags: EditText
    private lateinit var tvSelectedTrip: TextView
    private lateinit var btnSelectTrip: Button
    private lateinit var btnSelectImage: Button
    private lateinit var btnPost: Button
    private lateinit var progress: ProgressBar
    private lateinit var recyclerImages: RecyclerView

    private val uploadedImageUrls = mutableListOf<String>()   // URL sau upload -> gửi backend
    private lateinit var imageAdapter: ImagePreviewAdapter

    private var selectedTripId: String? = null
    private var selectedTripTitle: String? = null

    // Pick multiple images
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult
            uploadImagesToFirebase(uris)
        }

    // Receive trip from SelectTripForPostActivity
    private val selectTripLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult

            selectedTripId = data.getStringExtra("tripId")
            selectedTripTitle = data.getStringExtra("tripTitle")

            tvSelectedTrip.text = selectedTripTitle?.let { "Trip: $it" } ?: "Đã chọn chuyến đi"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        bindViews()
        setupRecycler()
        observeVM()

        btnSelectImage.setOnClickListener {
            // Chọn nhiều ảnh
            pickImagesLauncher.launch("image/*")
        }

        btnSelectTrip.setOnClickListener {
            // Mở màn chọn trip của bạn
            val i = Intent(this, SelectTripForPostActivity::class.java)
            // nếu activity này cần userId thì truyền thêm:
            intent.getStringExtra("userId")?.let { i.putExtra("userId", it) }
            selectTripLauncher.launch(i)
        }

        btnPost.setOnClickListener { submitPost() }
    }

    private fun bindViews() {
        edtTitle = findViewById(R.id.edtTitle)
        edtContent = findViewById(R.id.edtContent)
        edtTags = findViewById(R.id.edtTags)
        tvSelectedTrip = findViewById(R.id.tvSelectedTrip)
        btnSelectTrip = findViewById(R.id.btnSelectTrip)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnPost = findViewById(R.id.btnPost)
        progress = findViewById(R.id.progressCreatePost)
        recyclerImages = findViewById(R.id.recyclerImages)
    }

    private fun setupRecycler() {
        imageAdapter = ImagePreviewAdapter()
        recyclerImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerImages.adapter = imageAdapter
    }

    private fun observeVM() {
        viewModel.isPosting.observe(this) { loading ->
            progress.isVisible = loading
            btnPost.isEnabled = !loading
            btnSelectImage.isEnabled = !loading
            btnSelectTrip.isEnabled = !loading
        }

        viewModel.postCreated.observe(this) { created ->
            if (created != null) {
                Toast.makeText(this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) {
            if (!it.isNullOrBlank()) Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Upload xong mới show preview URL (yêu cầu của bạn)
     */
    private fun uploadImagesToFirebase(uris: List<Uri>) {
        progress.isVisible = true
        btnSelectImage.isEnabled = false
        btnPost.isEnabled = false

        val storage = FirebaseStorage.getInstance().reference
        var done = 0
        val total = uris.size

        // nếu muốn chọn lại thì clear trước
        uploadedImageUrls.clear()
        imageAdapter.submitUrls(emptyList())

        uris.forEach { uri ->
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.child("posts/$fileName")

            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        uploadedImageUrls.add(url.toString())

                        // ✅ chỉ show preview sau khi có downloadUrl
                        imageAdapter.submitUrls(uploadedImageUrls.toList())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Upload ảnh thất bại!", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    done++
                    if (done == total) {
                        progress.isVisible = false
                        btnSelectImage.isEnabled = true
                        btnPost.isEnabled = true
                    }
                }
        }
    }

    private fun submitPost() {
        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "Thiếu userId", Toast.LENGTH_SHORT).show()
            return
        }

        val title = edtTitle.text.toString().trim()
        val content = edtContent.text.toString().trim()

        // ✅ validate
        if (title.isBlank()) {
            Toast.makeText(this, "Tiêu đề không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        if (uploadedImageUrls.isEmpty()) {
            Toast.makeText(this, "Bạn phải chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val tags = edtTags.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { null }  // ✅ đúng format backend (List<String> hoặc null)

        val req = CreatePostRequest(
            userId = userId,
            title = title,
            content = content,
            images = uploadedImageUrls.toList(),
            tags = tags,
            tripId = selectedTripId // ✅ nhận từ SelectTripForPostActivity
        )

        viewModel.createPost(req)
    }

    companion object {

        const val EXTRA_USER_ID = "userId"
        const val EXTRA_TRIP_ID = "tripId"

        // dùng khi KHÔNG cần result
        fun start(activity: Activity, userId: String, tripId: String? = null) {
            if (userId.isBlank()) return

            val i = Intent(activity, CreatePostActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            activity.startActivity(i)
        }
    }
}

/**
 * Preview adapter: hiển thị URL ảnh bằng Glide
 */
private class ImagePreviewAdapter : RecyclerView.Adapter<ImagePreviewAdapter.VH>() {

    private val urls = mutableListOf<String>()

    fun submitUrls(newUrls: List<String>) {
        urls.clear()
        urls.addAll(newUrls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val iv = ImageView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(160, 160).apply {
                marginEnd = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return VH(iv)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = urls[position]
        Glide.with(holder.imageView).load(url).into(holder.imageView)
    }

    class VH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}

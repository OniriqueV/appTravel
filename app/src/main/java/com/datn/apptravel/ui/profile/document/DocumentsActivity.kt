package com.datn.apptravels.ui.profile.documents

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.datn.apptravels.data.model.Document
import com.datn.apptravels.data.repository.DocumentRepository
import com.datn.apptravels.databinding.ActivityDocumentsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream

class DocumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentsBinding
    private val documentRepository: DocumentRepository by inject()
    private val auth: FirebaseAuth by inject()

    private lateinit var adapter: DocumentsAdapter
    private val documents = mutableListOf<Document>()

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFilePicker()
        } else {
            showToast("Cần cấp quyền để chọn file")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadDocuments()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        adapter = DocumentsAdapter(
            documents = documents,
            onItemClick = { document -> viewDocument(document) },
            onDeleteClick = { document -> confirmDeleteDocument(document) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabUpload.setOnClickListener {
            checkPermissionAndPickFile()
        }
    }

    private fun checkPermissionAndPickFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openFilePicker()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openFilePicker() {
        pickFileLauncher.launch("*/*")
    }

    private fun handleFileSelection(uri: Uri) {
        val fileName = getFileName(uri)
        val fileType = getFileType(uri)
        val fileSize = getFileSize(uri)

        // Check file size (max 700KB)
        if (fileSize > 700 * 1024) {
            showToast("File quá lớn. Kích thước tối đa: 700KB")
            return
        }

        showUploadDialog(uri, fileName, fileType)
    }

    private fun showUploadDialog(uri: Uri, fileName: String, fileType: String) {
        val categories = arrayOf("Vé máy bay", "Booking khách sạn", "Lịch trình", "Visa", "Khác")
        var selectedCategory = "OTHER"

        MaterialAlertDialogBuilder(this)
            .setTitle("Tải lên tài liệu")
            .setMessage("File: $fileName\nKích thước: ${formatFileSize(getFileSize(uri))}")
            .setSingleChoiceItems(categories, 4) { _, which ->
                selectedCategory = when (which) {
                    0 -> "TICKET"
                    1 -> "BOOKING"
                    2 -> "ITINERARY"
                    3 -> "VISA"
                    else -> "OTHER"
                }
            }
            .setPositiveButton("Tải lên") { _, _ ->
                uploadDocument(uri, fileName, fileType, selectedCategory)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uploadDocument(
        uri: Uri,
        fileName: String,
        fileType: String,
        category: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = documentRepository.uploadDocument(
                userId = userId,
                fileUri = uri,
                fileName = fileName,
                fileType = fileType,
                category = category,
                description = null,
                tripId = null
            )

            binding.progressBar.visibility = View.GONE

            result.onSuccess {
                showToast("Tải lên thành công")
                loadDocuments()
            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun loadDocuments() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = documentRepository.getUserDocuments(userId)

            binding.progressBar.visibility = View.GONE

            result.onSuccess { docs ->
                documents.clear()
                documents.addAll(docs)
                adapter.notifyDataSetChanged()

                binding.emptyState.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (docs.isEmpty()) View.GONE else View.VISIBLE
            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun viewDocument(document: Document) {
        when (document.fileType) {
            "IMAGE" -> showImageDialog(document)
            "PDF" -> showPdfOptions(document)
            else -> showToast("Loại file không được hỗ trợ để xem")
        }
    }

    private fun showImageDialog(document: Document) {
        // Decode Base64 to Bitmap
        val bytes = Base64.decode(document.fileBase64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        if (bitmap != null) {
            // Show in dialog or new activity
            val intent = Intent(this, ImageViewerActivity::class.java)
            intent.putExtra("document_id", document.id)
            intent.putExtra("file_name", document.fileName)
            startActivity(intent)
        } else {
            showToast("Không thể hiển thị ảnh")
        }
    }

    private fun showPdfOptions(document: Document) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Tùy chọn PDF")
            .setMessage("Bạn muốn làm gì với file PDF này?")
            .setPositiveButton("Lưu vào thiết bị") { _, _ ->
                savePdfToDevice(document)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun savePdfToDevice(document: Document) {
        try {
            val bytes = Base64.decode(document.fileBase64, Base64.DEFAULT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, document.fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(bytes)
                    }
                    showToast("Đã lưu vào thư mục Downloads")
                }
            } else {
                // Android 9 and below
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(downloadsDir, document.fileName)
                FileOutputStream(file).use { it.write(bytes) }
                showToast("Đã lưu vào: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            showToast("Lỗi khi lưu file: ${e.message}")
        }
    }

    private fun confirmDeleteDocument(document: Document) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa tài liệu")
            .setMessage("Bạn có chắc muốn xóa tài liệu này?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteDocument(document)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteDocument(document: Document) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = documentRepository.deleteDocument(document.id)

            binding.progressBar.visibility = View.GONE

            result.onSuccess {
                showToast("Đã xóa tài liệu")
                loadDocuments()
            }.onFailure {
                showToast("Lỗi: ${it.message}")
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
        return result
    }

    private fun getFileType(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return when {
            mimeType?.startsWith("image/") == true -> "IMAGE"
            mimeType == "application/pdf" -> "PDF"
            else -> "OTHER"
        }
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024
        if (kb < 1024) return "$kb KB"
        val mb = kb / 1024
        return String.format("%.2f MB", mb.toFloat())
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
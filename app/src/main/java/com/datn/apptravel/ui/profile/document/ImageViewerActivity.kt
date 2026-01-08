package com.datn.apptravels.ui.profile.documents

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravels.databinding.ActivityImageViewerBinding
import com.datn.apptravels.data.repository.DocumentRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private val documentRepository: DocumentRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val documentId = intent.getStringExtra("document_id") ?: ""
        val fileName = intent.getStringExtra("file_name") ?: ""

        setupUI(fileName)
        loadImage(documentId)
    }

    private fun setupUI(fileName: String) {
        binding.tvFileName.text = fileName
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadImage(documentId: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = documentRepository.getDocumentById(documentId)

            binding.progressBar.visibility = View.GONE

            result.onSuccess { document ->
                try {
                    // Decode Base64 to Bitmap
                    val bytes = Base64.decode(document.fileBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    if (bitmap != null) {
                        binding.photoView.setImageBitmap(bitmap)
                    } else {
                        showError("Không thể hiển thị ảnh")
                    }
                } catch (e: Exception) {
                    showError("Lỗi: ${e.message}")
                }
            }.onFailure {
                showError("Lỗi tải ảnh: ${it.message}")
            }
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }
}
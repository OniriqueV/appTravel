package com.datn.apptravels.ui.profile.documents

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.datn.apptravels.R
import com.datn.apptravels.data.model.Document
import com.datn.apptravels.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentsAdapter(
    private val documents: List<Document>,
    private val onItemClick: (Document) -> Unit,
    private val onDeleteClick: (Document) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                // Set title (main heading - bold)
                if (document.title.isNotBlank()) {
                    tvDocumentTitle.text = document.title
                    tvDocumentTitle.visibility = View.VISIBLE
                    // Show filename below title in smaller font
                    tvFileName.text = document.fileName
                    tvFileName.visibility = View.VISIBLE
                } else {
                    // If no title, show filename as main heading
                    tvDocumentTitle.text = document.fileName
                    tvDocumentTitle.visibility = View.VISIBLE
                    tvFileName.visibility = View.GONE
                }

                // Set category
                tvCategory.text = getCategoryName(document.category)

                // Set file type icon and thumbnail
                when (document.fileType) {
                    "IMAGE" -> {
                        // Show thumbnail for images
                        try {
                            val bytes = Base64.decode(document.fileBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) {
                                ivFileIcon.setImageBitmap(bitmap)
                            } else {
                                ivFileIcon.setImageResource(R.drawable.img_sample_2)
                            }
                        } catch (e: Exception) {
                            ivFileIcon.setImageResource(R.drawable.img_sample_2)
                        }
                    }
                    "PDF" -> {
                        ivFileIcon.setImageResource(R.drawable.ic_pdf)
                    }
                    else -> {
                        ivFileIcon.setImageResource(R.drawable.ic_file)
                    }
                }

                // Set file size
                tvFileSize.text = formatFileSize(document.fileSize)

                // Set date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(document.createdAt))

                // Set description if available
                if (!document.description.isNullOrEmpty()) {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = "Ghi chú: ${document.description}"
                } else {
                    tvDescription.visibility = View.GONE
                }

                // Click listeners
                root.setOnClickListener { onItemClick(document) }
                btnDelete.setOnClickListener { onDeleteClick(document) }
            }
        }

        private fun getCategoryName(category: String): String {
            return when (category) {
                "TICKET" -> "Vé máy bay"
                "BOOKING" -> "Booking"
                "ITINERARY" -> "Lịch trình"
                "VISA" -> "Visa"
                else -> "Khác"
            }
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024
            if (kb < 1024) return "$kb KB"
            val mb = kb / 1024
            return String.format("%.2f MB", mb.toFloat())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documents[position])
    }

    override fun getItemCount(): Int = documents.size
}
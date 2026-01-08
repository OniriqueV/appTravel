package com.datn.apptravels.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.datn.apptravels.data.model.Document
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class DocumentRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val documentsCollection = firestore.collection("documents")

    // Maximum file size configuration
    private val MAX_FILE_SIZE_BYTES = 1024 * 1024 // 1MB original file
    private val MAX_BASE64_SIZE_BYTES = 900 * 1024 // ~900KB Base64 (safe under 1MB limit)

    suspend fun uploadDocument(
        userId: String,
        fileUri: Uri,
        title: String,
        fileName: String,
        fileType: String,
        category: String,
        description: String?,
        tripId: String?
    ): Result<Document> {
        return try {
            // Read file to bytes
            val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
            var bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                return Result.failure(Exception("Không thể đọc file"))
            }

            // Auto-compress images if too large
            if (fileType == "IMAGE" && bytes.size > MAX_FILE_SIZE_BYTES) {
                bytes = compressImage(bytes, MAX_FILE_SIZE_BYTES)
                    ?: return Result.failure(Exception("Không thể nén ảnh"))
            }

            // Check file size after compression
            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                val sizeMB = String.format("%.2f", bytes.size / (1024f * 1024f))
                return Result.failure(Exception("File quá lớn ($sizeMB MB). Kích thước tối đa: 1 MB"))
            }

            // Convert to Base64
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

            // Double check Base64 size
            val base64SizeBytes = base64String.toByteArray().size
            if (base64SizeBytes > MAX_BASE64_SIZE_BYTES) {
                val sizeMB = String.format("%.2f", base64SizeBytes / (1024f * 1024f))
                return Result.failure(Exception("File sau mã hóa quá lớn ($sizeMB MB). Vui lòng chọn file nhỏ hơn"))
            }

            // Create document object
            val document = Document(
                id = documentsCollection.document().id,
                userId = userId,
                title = title,
                fileName = fileName,
                fileBase64 = base64String,
                fileType = fileType,
                fileSize = bytes.size.toLong(),
                category = category,
                description = description,
                tripId = tripId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save document info to Firestore
            documentsCollection.document(document.id).set(document.toMap()).await()

            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compress image to target size
     * @param bytes Original image bytes
     * @param maxSize Target size in bytes
     * @return Compressed image bytes or null if failed
     */
    private fun compressImage(bytes: ByteArray, maxSize: Int): ByteArray? {
        return try {
            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null

            // Start with high quality
            var quality = 90
            var compressedBytes: ByteArray

            // Calculate initial scale to reduce dimensions if image is very large
            val maxDimension = 2048 // Max width or height
            val scale = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                maxDimension.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)
            } else {
                1f
            }

            // Resize if needed
            val bitmap = if (scale < 1f) {
                val newWidth = (originalBitmap.width * scale).toInt()
                val newHeight = (originalBitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true).also {
                    if (it != originalBitmap) originalBitmap.recycle()
                }
            } else {
                originalBitmap
            }

            // Compress with decreasing quality until size is acceptable
            do {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                compressedBytes = stream.toByteArray()
                quality -= 10

                // Log compression progress
                android.util.Log.d("DocumentRepository",
                    "Compression: size=${compressedBytes.size / 1024}KB, quality=$quality")

            } while (compressedBytes.size > maxSize && quality > 10)

            bitmap.recycle()

            if (compressedBytes.size <= maxSize) {
                android.util.Log.d("DocumentRepository",
                    "Compression successful: ${bytes.size / 1024}KB → ${compressedBytes.size / 1024}KB")
                compressedBytes
            } else {
                android.util.Log.e("DocumentRepository", "Failed to compress to target size")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Error compressing image: ${e.message}")
            null
        }
    }

    suspend fun getUserDocuments(userId: String): Result<List<Document>> {
        return try {
            val snapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Sort in memory instead of using Firestore orderBy
            val documents = snapshot.documents
                .mapNotNull { doc -> doc.toObject(Document::class.java) }
                .sortedByDescending { it.createdAt }

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentsByTrip(tripId: String): Result<List<Document>> {
        return try {
            val snapshot = documentsCollection
                .whereEqualTo("tripId", tripId)
                .get()
                .await()

            // Sort in memory instead of using Firestore orderBy
            val documents = snapshot.documents
                .mapNotNull { doc -> doc.toObject(Document::class.java) }
                .sortedByDescending { it.createdAt }

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            // Only delete from Firestore (no Storage cleanup needed)
            documentsCollection.document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(documentId: String): Result<Document> {
        return try {
            val snapshot = documentsCollection.document(documentId).get().await()
            val document = snapshot.toObject(Document::class.java)

            if (document != null) {
                Result.success(document)
            } else {
                Result.failure(Exception("Document not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to convert Base64 to ByteArray
    fun base64ToByteArray(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }
}
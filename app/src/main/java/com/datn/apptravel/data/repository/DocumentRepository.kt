package com.datn.apptravels.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.datn.apptravels.data.model.Document
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DocumentRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val documentsCollection = firestore.collection("documents")

    // Maximum file size: 5MB (to avoid Firestore document size limit of ~1MB)
    // Note: Base64 increases size by ~33%, so 700KB file -> ~930KB Base64
    private val MAX_FILE_SIZE = 700 * 1024 // 700KB

    suspend fun uploadDocument(
        userId: String,
        fileUri: Uri,
        fileName: String,
        fileType: String,
        category: String,
        description: String?,
        tripId: String?
    ): Result<Document> {
        return try {
            // Read file to bytes
            val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                return Result.failure(Exception("Không thể đọc file"))
            }

            // Check file size
            if (bytes.size > MAX_FILE_SIZE) {
                return Result.failure(Exception("File quá lớn. Kích thước tối đa: 700KB"))
            }

            // Convert to Base64
            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

            // Create document object
            val document = Document(
                id = documentsCollection.document().id,
                userId = userId,
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

    suspend fun getUserDocuments(userId: String): Result<List<Document>> {
        return try {
            val snapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val documents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Document::class.java)
            }

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentsByTrip(tripId: String): Result<List<Document>> {
        return try {
            val snapshot = documentsCollection
                .whereEqualTo("tripId", tripId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val documents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Document::class.java)
            }

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
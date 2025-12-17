package com.datn.apptravel.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.datn.apptravel.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {

    private val usersCollection = firestore.collection("users")

    suspend fun getUserById(uid: String): User? {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        try {
            val updateData = updates.toMutableMap()
            updateData["updatedAt"] = System.currentTimeMillis()

            Log.d("UserRepository", "Updating user $uid")
            usersCollection.document(uid).update(updateData).await()
            Log.d("UserRepository", "User updated successfully")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user: ${e.message}", e)
            throw e
        }
    }

    suspend fun convertImageToBase64(imageUri: Uri): String {
        return try {
            Log.d("UserRepository", "Converting image to Base64...")

            // Mở input stream từ URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)

            if (inputStream == null) {
                throw Exception("Không thể đọc file ảnh")
            }

            // Decode bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                throw Exception("Không thể decode ảnh")
            }

            // Resize ảnh để giảm kích thước (max 800x800)
            val resizedBitmap = resizeBitmap(originalBitmap, 800, 800)

            // Convert to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Tính kích thước
            val sizeInKB = byteArray.size / 1024
            Log.d("UserRepository", "Image size: $sizeInKB KB")

            if (sizeInKB > 1024) {
                Log.w("UserRepository", "Image size is large: $sizeInKB KB, consider reducing quality")
            }

            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            val dataUrl = "data:image/jpeg;base64,$base64String"

            Log.d("UserRepository", "Image converted successfully")

            // Cleanup
            originalBitmap.recycle()
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }

            dataUrl
        } catch (e: Exception) {
            Log.e("UserRepository", "Error converting image: ${e.message}", e)
            throw Exception("Lỗi khi xử lý ảnh: ${e.message}")
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Nếu ảnh đã nhỏ hơn max size, không cần resize
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }
}
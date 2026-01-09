package com.datn.apptravels.data.repository

import android.content.Context
import android.net.Uri
import com.datn.apptravels.data.api.TripApiService
import com.datn.apptravels.data.model.Plan
import com.datn.apptravels.data.model.PlanType
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.model.request.*
import com.datn.apptravels.ui.discover.model.CommentDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class TripRepository(private val tripApiService: TripApiService) {
    
    suspend fun createTrip(request: CreateTripRequest): Result<Trip> {
        return try {
            val response = tripApiService.createTrip(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val trip = response.body()?.data
                if (trip != null) {
                    Result.success(trip)
                } else {
                    Result.failure(Exception("Trip data is null"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Failed to create trip"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTripById(tripId: String): Result<Trip> {
        return try {
            val response = tripApiService.getTripById(tripId)
            if (response.isSuccessful && response.body()?.success == true) {
                val trip = response.body()?.data
                if (trip != null) {
                    Result.success(trip)
                } else {
                    Result.failure(Exception("Trip not found"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Failed to get trip"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get trip with full plan details - optimized method
     * Returns trip WITH all complete plan information in 1 API call
     * Use this when you need to display all plan details immediately
     */
    suspend fun getTripWithFullPlans(tripId: String): Result<Trip> {
        return try {
            val response = tripApiService.getTripWithFullPlans(tripId)
            if (response.isSuccessful && response.body()?.success == true) {
                val trip = response.body()?.data
                if (trip != null) {
                    Result.success(trip)
                } else {
                    Result.failure(Exception("Trip not found"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Failed to get trip with plans"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): Result<com.datn.apptravels.data.model.User> {
        return try {
            val response = tripApiService.getUserById(userId)
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User not found"))
                }
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTripsByUserId(userId: String): Result<List<Trip>> {
        return try {
            val response = tripApiService.getTripsByUserId(userId)
            if (response.isSuccessful) {
                val trips = response.body() ?: emptyList()
                Result.success(trips)
            } else {
                Result.failure(Exception("Failed to get trips"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTripsByMemberId(userId: String): Result<List<Trip>> {
        return try {
            val response = tripApiService.getTripsByMemberId(userId)
            if (response.isSuccessful) {
                val trips = response.body() ?: emptyList()
                Result.success(trips)
            } else {
                Result.failure(Exception("Failed to get member trips"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTrip(tripId: String, request: CreateTripRequest): Result<Trip> {
        return try {
            val response = tripApiService.updateTrip(tripId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val trip = response.body()?.data
                if (trip != null) {
                    Result.success(trip)
                } else {
                    Result.failure(Exception("Trip data is null"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Failed to update trip"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            val response = tripApiService.deleteTrip(tripId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete trip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePlan(tripId: String, planId: String): Result<Unit> {
        return try {
            val response = tripApiService.deletePlan(tripId, planId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePhotoFromPlan(tripId: String, planId: String, photoFileName: String): Result<Unit> {
        return try {
            val response = tripApiService.deletePhotoFromPlan(tripId, planId, photoFileName)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete photo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Plan methods
//    suspend fun createPlan(tripId: String, request: CreatePlanRequest): Result<Plan> {
//        return try {
//            val response = tripApiService.createPlan(tripId, request)
//            if (response.isSuccessful) {
//                val plan = response.body()
//                if (plan != null) {
//                    Result.success(plan)
//                } else {
//                    Result.failure(Exception("Plan data is null"))
//                }
//            } else {
//                Result.failure(Exception("Failed to create plan"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
    
    suspend fun createFlightPlan(tripId: String, request: CreateFlightPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createFlightPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Flight plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create flight plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateFlightPlan(tripId: String, planId: String, request: CreateFlightPlanRequest): Result<Plan> {
        return try {
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update flight plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createRestaurantPlan(tripId: String, request: CreateRestaurantPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createRestaurantPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create restaurant plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateRestaurantPlan(tripId: String, planId: String, request: CreateRestaurantPlanRequest): Result<Plan> {
        return try {
            // Convert specialized request to generic CreatePlanRequest for update
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update restaurant plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createLodgingPlan(tripId: String, request: CreateLodgingPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createLodgingPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Lodging plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create lodging plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateLodgingPlan(tripId: String, planId: String, request: CreateLodgingPlanRequest): Result<Plan> {
        return try {
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update lodging plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createActivityPlan(tripId: String, request: CreateActivityPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createActivityPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Activity plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create activity plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateActivityPlan(tripId: String, planId: String, request: CreateActivityPlanRequest): Result<Plan> {
        return try {
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update activity plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createBoatPlan(tripId: String, request: CreateBoatPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createBoatPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Boat plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create boat plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBoatPlan(tripId: String, planId: String, request: CreateBoatPlanRequest): Result<Plan> {
        return try {
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update boat plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createCarRentalPlan(tripId: String, request: CreateCarRentalPlanRequest): Result<Plan> {
        return try {
            val response = tripApiService.createCarRentalPlan(tripId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Car rental plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to create car rental plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateCarRentalPlan(tripId: String, planId: String, request: CreateCarRentalPlanRequest): Result<Plan> {
        return try {
            val updateRequest = CreatePlanRequest(
                tripId = request.tripId,
                title = request.title,
                address = request.address,
                location = request.location,
                startTime = request.startTime,
                endTime = request.endTime,
                expense = request.expense,
                photoUrl = request.photoUrl,
                type = request.type
            )
            val response = tripApiService.updatePlan(tripId, planId, updateRequest)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update car rental plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlansByTripId(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body() ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlanById(tripId: String, planId: String): Result<Plan> {
        return try {
            val response = tripApiService.getPlanById(tripId, planId)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan not found"))
                }
            } else {
                Result.failure(Exception("Failed to get plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePlanPhotos(tripId: String, planId: String, photos: List<String>): Result<Plan> {
        return try {
            val request = CreatePlanRequest(
                tripId = tripId,
                photos = photos
            )
            val response = tripApiService.updatePlan(tripId, planId, request)
            if (response.isSuccessful) {
                val plan = response.body()
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Plan data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update plan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload image to server
    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> {
        return try {
            // Convert Uri to File
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            // Create MultipartBody.Part
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
            
            // Upload
            val response = tripApiService.uploadImage(multipartBody)
            
            // Clean up temp file
            file.delete()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val fileName = response.body()?.fileName
                if (fileName != null) {
                    Result.success(fileName)
                } else {
                    Result.failure(Exception("File name is null"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Failed to upload image"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Upload multiple images
    suspend fun uploadImages(context: Context, imageUris: List<Uri>): Result<List<String>> {
        return try {
            val parts = mutableListOf<MultipartBody.Part>()
            val tempFiles = mutableListOf<File>()
            
            // Convert all Uris to MultipartBody.Part
            imageUris.forEachIndexed { index, uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$index.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                tempFiles.add(file)
                
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("files", file.name, requestBody)
                parts.add(multipartBody)
            }
            
            // Upload
            val response = tripApiService.uploadImages(parts)
            
            // Clean up temp files
            tempFiles.forEach { it.delete() }
            
            if (response.isSuccessful) {
                val fileNames = response.body()?.mapNotNull { it.fileName } ?: emptyList()
                Result.success(fileNames)
            } else {
                Result.failure(Exception("Failed to upload images"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndUploadImage(context: Context, imageUrl: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("TripRepository", "Starting download from: $imageUrl")
                
                // Download image from URL
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val file = File(context.cacheDir, "downloaded_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                android.util.Log.d("TripRepository", "Downloaded image to: ${file.absolutePath}, size: ${file.length()} bytes")
                
                // Upload to server
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
                
                android.util.Log.d("TripRepository", "Uploading to server...")
                val response = tripApiService.uploadImage(multipartBody)
                
                // Clean up temp file
                file.delete()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val fileName = response.body()?.fileName
                    if (fileName != null) {
                        android.util.Log.d("TripRepository", "✓ Upload successful! Filename: $fileName")
                        Result.success(fileName)
                    } else {
                        android.util.Log.e("TripRepository", "✗ File name is null in response")
                        Result.failure(Exception("File name is null"))
                    }
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to upload image"
                    android.util.Log.e("TripRepository", "✗ Upload failed: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                android.util.Log.e("TripRepository", "✗ Error downloading and uploading image: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getAllPlansForTrip(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body() ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun getRestaurantDetails(tripId: String): Result<List<Plan>> {
//        return try {
//            val response = tripApiService.getPlansByTripId(tripId)
//            if (response.isSuccessful) {
//                val plans = response.body()?.filter {
//                    it.type == PlanType.RESTAURANT
//                } ?: emptyList()
//                Result.success(plans)
//            } else {
//                Result.failure(Exception("Failed to get restaurant plans"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    suspend fun getLodgingDetails(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body()?.filter {
                    it.type == PlanType.LODGING
                } ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get lodging plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityDetails(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body()?.filter {
                    val type = it.type
                    type == PlanType.ACTIVITY || type == PlanType.TOUR || type == PlanType.THEATER ||
                            type == PlanType.SHOPPING || type == PlanType.CAMPING || type == PlanType.RELIGION
                } ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get activity plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlightDetails(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body()?.filter {
                    it.type == PlanType.FLIGHT
                } ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get flight plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBoatDetails(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body()?.filter {
                    it.type== PlanType.BOAT
                } ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get boat plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get car rental plans
     */
    suspend fun getCarRentalDetails(tripId: String): Result<List<Plan>> {
        return try {
            val response = tripApiService.getPlansByTripId(tripId)
            if (response.isSuccessful) {
                val plans = response.body()?.filter {
                    val type = it.type
                    type == PlanType.CAR_RENTAL || type == PlanType.TRAIN
                } ?: emptyList()
                Result.success(plans)
            } else {
                Result.failure(Exception("Failed to get car rental plans"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sharing management methods
    suspend fun updateSharedUsers(tripId: String, sharedUserIds: List<String>, currentUserId: String): Result<Trip> {
        return try {
            val request = UpdateSharedUsersRequest(sharedWithUserIds = sharedUserIds)
            val response = tripApiService.updateSharedUsers(tripId, request, currentUserId)
            if (response.isSuccessful) {
                val trip = response.body()
                if (trip != null) {
                    Result.success(trip)
                } else {
                    Result.failure(Exception("Trip data is null"))
                }
            } else {
                Result.failure(Exception("Failed to update shared users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(userId: String): Result<List<com.datn.apptravels.data.model.User>> {
        return try {
            val response = tripApiService.getFollowers(userId)
            if (response.isSuccessful) {
                val followers = response.body() ?: emptyList()
                Result.success(followers)
            } else {
                Result.failure(Exception("Failed to get followers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Plan comments methods
    suspend fun getComments(planId: String): Result<List<CommentDto>> {
        return try {
            val response = tripApiService.getComments(planId)
            if (response.isSuccessful) {
                val comments = response.body() ?: emptyList()
                Result.success(comments)
            } else {
                Result.failure(Exception("Failed to get comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun postComment(planId: String, userId: String, content: String, parentId: String? = null): Result<Unit> {
        return try {
            val body = mutableMapOf("content" to content)
            if (parentId != null) {
                body["parentId"] = parentId
            }
            
            // Retry logic for 502 errors (Bad Gateway)
            var lastException: Exception? = null
            var successResult: Result<Unit>? = null
            
            for (attempt in 0 until 3) {
                try {
                    val response = tripApiService.postComment(planId, userId, body)
                    if (response.isSuccessful) {
                        successResult = Result.success(Unit)
                        break
                    } else if (response.code() == 502 && attempt < 2) {
                        // Retry on 502 error (except last attempt)
                        kotlinx.coroutines.delay(1000L * (attempt + 1)) // Exponential backoff
                        lastException = Exception("Server temporarily unavailable (502). Retrying...")
                    } else {
                        // For other errors or last attempt, fail
                        val errorMsg = when (response.code()) {
                            502 -> "Server is temporarily unavailable. Please try again later."
                            500 -> "Server error. Please try again later."
                            404 -> "Plan not found."
                            else -> "Failed to post comment (${response.code()})"
                        }
                        lastException = Exception(errorMsg)
                        break
                    }
                } catch (e: Exception) {
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(1000L * (attempt + 1))
                        lastException = e
                    } else {
                        lastException = e
                        break
                    }
                }
            }
            
            // Return success if we got it, otherwise return failure
            successResult ?: Result.failure(lastException ?: Exception("Failed to post comment after retries"))
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Get Adventure trips - optimized endpoint
     * Returns all trip and user data in 1 API call
     * Replaces 11 separate API calls
     */
    suspend fun getAdventureTrips(userId: String?, limit: Int = 10): Result<com.datn.apptravels.data.model.AdventureResponse> {
        return try {
            val response = tripApiService.getAdventureTrips(userId, limit)
            if (response.isSuccessful) {
                val adventureResponse = response.body()
                if (adventureResponse != null) {
                    Result.success(adventureResponse)
                } else {
                    Result.failure(Exception("Adventure response is null"))
                }
            } else {
                Result.failure(Exception("Failed to get adventure trips"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.datn.apptravel.data.repository

import android.content.Context
import android.net.Uri
import com.datn.apptravel.data.api.TripApiService
import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.model.request.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

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
}

package com.datn.apptravel.data.api


import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.model.request.*
import com.datn.apptravel.data.model.response.FileUploadResponse
import com.datn.apptravel.data.model.response.TripResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface TripApiService {
    
    @POST("api/trips")
    suspend fun createTrip(
        @Body request: CreateTripRequest
    ): Response<TripResponse>
    
    @GET("api/trips/{id}")
    suspend fun getTripById(
        @Path("id") tripId: String
    ): Response<TripResponse>
    
    @GET("api/trips/user/{userId}")
    suspend fun getTripsByUserId(
        @Path("userId") userId: String
    ): Response<List<Trip>>
    
    @PUT("api/trips/{id}")
    suspend fun updateTrip(
        @Path("id") tripId: String,
        @Body request: CreateTripRequest
    ): Response<TripResponse>
    
    @DELETE("api/trips/{id}")
    suspend fun deleteTrip(
        @Path("id") tripId: String
    ): Response<Void>
    
    // Plans endpoints
//    @POST("api/trips/{tripId}/plans")
//    suspend fun createPlan(
//        @Path("tripId") tripId: String,
//        @Body request: CreatePlanRequest
//    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/flight")
    suspend fun createFlightPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateFlightPlanRequest
    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/restaurant")
    suspend fun createRestaurantPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateRestaurantPlanRequest
    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/lodging")
    suspend fun createLodgingPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateLodgingPlanRequest
    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/activity")
    suspend fun createActivityPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateActivityPlanRequest
    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/boat")
    suspend fun createBoatPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateBoatPlanRequest
    ): Response<Plan>
    
    @POST("api/trips/{tripId}/plans/car-rental")
    suspend fun createCarRentalPlan(
        @Path("tripId") tripId: String,
        @Body request: CreateCarRentalPlanRequest
    ): Response<Plan>
    
    @GET("api/trips/{tripId}/plans")
    suspend fun getPlansByTripId(
        @Path("tripId") tripId: String
    ): Response<List<Plan>>
    
    @GET("api/trips/{tripId}/plans/{planId}")
    suspend fun getPlanById(
        @Path("tripId") tripId: String,
        @Path("planId") planId: String
    ): Response<Plan>
    
    @PUT("api/trips/{tripId}/plans/{planId}")
    suspend fun updatePlan(
        @Path("tripId") tripId: String,
        @Path("planId") planId: String,
        @Body request: CreatePlanRequest
    ): Response<Plan>
    
    @DELETE("api/trips/{tripId}/plans/{planId}")
    suspend fun deletePlan(
        @Path("tripId") tripId: String,
        @Path("planId") planId: String
    ): Response<Void>
    
    @DELETE("api/trips/{tripId}/plans/{planId}/photos/{photoFileName}")
    suspend fun deletePhotoFromPlan(
        @Path("tripId") tripId: String,
        @Path("planId") planId: String,
        @Path("photoFileName") photoFileName: String
    ): Response<Void>
    
    // File upload endpoint
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<FileUploadResponse>
    
    @Multipart
    @POST("api/upload/images")
    suspend fun uploadImages(
        @Part files: List<MultipartBody.Part>
    ): Response<List<FileUploadResponse>>
}

package com.datn.apptravel.data.api


import com.datn.apptravel.data.model.Plan
import com.datn.apptravel.data.model.Trip
import com.datn.apptravel.data.model.request.CreatePlanRequest
import com.datn.apptravel.data.model.request.CreateTripRequest
import com.datn.apptravel.data.model.response.TripResponse
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
    @POST("api/trips/{tripId}/plans")
    suspend fun createPlan(
        @Path("tripId") tripId: String,
        @Body request: CreatePlanRequest
    ): Response<Plan>
    
    @GET("api/trips/{tripId}/plans")
    suspend fun getPlansByTripId(
        @Path("tripId") tripId: String
    ): Response<List<Plan>>
}

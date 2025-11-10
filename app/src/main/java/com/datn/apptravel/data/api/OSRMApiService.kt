package com.datn.apptravel.data.api

import com.datn.apptravel.data.model.response.OSRMResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OSRMApiService {
    
    /**
     * Get route from OSRM public API
     * @param coordinates format: "lon1,lat1;lon2,lat2;lon3,lat3"
     * @param overview full/simplified/false - return full geometry
     * @param geometries polyline/polyline6/geojson
     */
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true
    ): Response<OSRMResponse>
}

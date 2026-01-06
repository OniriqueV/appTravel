package com.datn.apptravels.data.model.response

import com.google.gson.annotations.SerializedName


data class GeoapifyResponse(
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("features")
    val features: List<PlaceFeature>?
)

data class PlaceFeature(
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("properties")
    val properties: PlaceProperties?,
    
    @SerializedName("geometry")
    val geometry: PlaceGeometry?
)

data class PlaceProperties(
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("country")
    val country: String?,
    
    @SerializedName("country_code")
    val countryCode: String?,
    
    @SerializedName("state")
    val state: String?,
    
    @SerializedName("city")
    val city: String?,
    
    @SerializedName("postcode")
    val postcode: String?,
    
    @SerializedName("street")
    val street: String?,
    
    @SerializedName("housenumber")
    val housenumber: String?,
    
    @SerializedName("lon")
    val lon: Double?,
    
    @SerializedName("lat")
    val lat: Double?,
    
    @SerializedName("formatted")
    val formatted: String?,
    
    @SerializedName("address_line1")
    val addressLine1: String?,
    
    @SerializedName("address_line2")
    val addressLine2: String?,
    
    @SerializedName("categories")
    val categories: List<String>?,
    
    @SerializedName("details")
    val details: List<String>?,
    
    @SerializedName("datasource")
    val datasource: PlaceDatasource?,
    
    @SerializedName("distance")
    val distance: Double?,
    
    @SerializedName("place_id")
    val placeId: String?
)

data class PlaceGeometry(
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("coordinates")
    val coordinates: List<Double>?
)

data class PlaceDatasource(
    @SerializedName("sourcename")
    val sourcename: String?,
    
    @SerializedName("attribution")
    val attribution: String?,
    
    @SerializedName("license")
    val license: String?,
    
    @SerializedName("url")
    val url: String?
)

/**
 * Simple model for displaying places on map
 */
data class MapPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val category: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val galleryImages: List<String>? = null
)

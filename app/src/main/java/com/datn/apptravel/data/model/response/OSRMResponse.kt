package com.datn.apptravel.data.model.response

import com.google.gson.JsonElement

data class OSRMResponse(
    val code: String,
    val routes: List<Route>?
)

data class Route(
    val geometry: JsonElement,
    val legs: List<Leg>,
    val distance: Double,
    val duration: Double
)

data class Leg(
    val steps: List<Step>,
    val distance: Double,
    val duration: Double
)

data class Step(
    val geometry: JsonElement,
    val maneuver: Maneuver,
    val distance: Double,
    val duration: Double
)

data class Maneuver(
    val location: List<Double>,
    val type: String
)

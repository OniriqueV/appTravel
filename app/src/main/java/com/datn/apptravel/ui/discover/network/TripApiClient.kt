package com.datn.apptravels.ui.discover.network

import com.datn.apptravels.data.api.RetrofitClient
import com.datn.apptravels.data.api.TripApiService

object TripApiClient {
    val api: TripApiService by lazy { RetrofitClient.tripApiService }
}

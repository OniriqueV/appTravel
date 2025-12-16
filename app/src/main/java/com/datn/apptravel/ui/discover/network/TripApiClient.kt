package com.datn.apptravel.ui.discover.network

import com.datn.apptravel.data.api.RetrofitClient
import com.datn.apptravel.data.api.TripApiService

object TripApiClient {
    val api: TripApiService by lazy { RetrofitClient.tripApiService }
}

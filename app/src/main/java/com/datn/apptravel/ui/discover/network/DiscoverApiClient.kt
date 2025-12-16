package com.datn.apptravel.ui.discover.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.datn.apptravel.BuildConfig



object DiscoverApiClient {

    private const val BASE_URL = BuildConfig.DISCOVER_SERVICE_BASE_URL




    val api: DiscoverApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiscoverApi::class.java)
    }
}

package com.datn.apptravel.data.api

import com.datn.apptravel.BuildConfig
import com.datn.apptravel.data.api.RetrofitClient.retrofit
import com.datn.apptravel.ui.discover.network.FollowApi
import com.datn.apptravel.ui.discover.network.PlanMapApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.datn.apptravel.ui.discover.network.ProfileApi




object RetrofitClient {
    private val GEOAPIFY_BASE_URL = BuildConfig.GEOAPIFY_BASE_URL
    private val AUTH_BASE_URL = BuildConfig.AUTH_BASE_URL
    private val TRIP_SERVICE_BASE_URL = BuildConfig.TRIP_SERVICE_BASE_URL
    private val GOOGLE_API_BASE_URL = BuildConfig.GOOGLE_API_BASE_URL

    private val DISCOVER_SERVICE_BASE_URL = BuildConfig.DISCOVER_SERVICE_BASE_URL


    private const val TIMEOUT = 30L // seconds

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEOAPIFY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    
    private val tripServiceRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TRIP_SERVICE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val discoverRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(DISCOVER_SERVICE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val followApi: FollowApi by lazy {
        discoverRetrofit.create(FollowApi::class.java)
    }
    val tripApiService: TripApiService by lazy {
        tripServiceRetrofit.create(TripApiService::class.java)
    }
    
    val notificationApiService: NotificationApiService by lazy {
        tripServiceRetrofit.create(NotificationApiService::class.java)
    }
    
    private val googleApiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val googleImageSearchService: GoogleImageSearchService by lazy {
        googleApiRetrofit.create(GoogleImageSearchService::class.java)
    }

    val profileApi: ProfileApi by lazy {
        discoverRetrofit.create(ProfileApi::class.java)
    }

    val planMapApi: PlanMapApi by lazy {
        discoverRetrofit.create(PlanMapApi::class.java)
    }

    fun <T> createDiscoverService(service: Class<T>): T {
        return discoverRetrofit.create(service)
    }

    inline fun <reified T> createService(): T = retrofit.create(T::class.java)
}
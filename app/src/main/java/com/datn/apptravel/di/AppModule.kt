package com.datn.apptravel.di

import com.datn.apptravel.data.api.*
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.repository.*
import com.datn.apptravel.ui.app.*
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.discover.PlanMap.PlanMapDetailViewModel
import com.datn.apptravel.ui.discover.network.*
import com.datn.apptravel.ui.discover.network.ProfileRepository
import com.datn.apptravel.ui.discover.profileFollow.ProfileUserViewModel
import com.datn.apptravel.ui.notification.NotificationViewModel
import com.datn.apptravel.ui.profile.ProfileViewModel
import com.datn.apptravel.ui.profile.edit.EditProfileViewModel
import com.datn.apptravel.ui.profile.password.ChangePasswordViewModel
import com.datn.apptravel.ui.trip.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.datn.apptravel.data.remote.GeoapifyService
import com.datn.apptravel.data.remote.GoogleImageService
import com.datn.apptravel.data.remote.GroqService


val appModule = module {

    /* ================= API CORE ================= */
    single { RetrofitClient.createService<ApiService>() }
    single { RetrofitClient.tripApiService }
    single { RetrofitClient.googleImageSearchService }

    /* ================= DISCOVER ================= */
    single { DiscoverApiClient.api }
    single { DiscoverRepository(get()) }

    /* ================= PLAN MAP DETAIL (🔥 QUAN TRỌNG) ================= */
    // ⚠️ TUYỆT ĐỐI KHÔNG dùng createService()
    single { RetrofitClient.planMapApi }              // ← discoverRetrofit (8080)
    single { PlanMapDetailRepository(get()) }
    viewModel { PlanMapDetailViewModel(get(), get()) }

    /* ================= FOLLOW ================= */
    single<FollowApi> { RetrofitClient.followApi }
    single { FollowRepository(get()) }

    /* ================= PROFILE (USER KHÁC) ================= */
    single<ProfileApi> { RetrofitClient.profileApi }
    single { ProfileRepository(get()) }

    /* ================= LOCAL / FIREBASE ================= */
    single { SessionManager(androidContext()) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    /* ================= OTHER REPOSITORIES ================= */
    single { AuthRepository() }
    single { UserRepository(get(), androidContext()) }
    single { NotificationRepository(get()) }
    single { PlacesRepository(get()) }
    single { TripRepository(get()) }
    single { ImageSearchRepository(get()) }

    // ================= AI / GEO / IMAGE =================
    single {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
    }

// Geoapify Service
    single {
        get<Retrofit.Builder>()
            .baseUrl("https://api.geoapify.com/")
            .build()
            .create(GeoapifyService::class.java)
    }

// Google Image Service
    single {
        get<Retrofit.Builder>()
            .baseUrl("https://www.googleapis.com/")
            .build()
            .create(GoogleImageService::class.java)
    }

// Groq AI Service - FAST & FREE
    single {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqService::class.java)
    }

// AI Repository (với Groq)
    single {
        AIRepository(
            geoapifyService = get(),
            googleImageService = get(),
            groqService = get()
        )
    }

    // ================= VIEWMODELS =================
    /* ================= VIEWMODELS ================= */
    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { DiscoverViewModel(get(), sessionManager = get()) }
    viewModel { NotificationViewModel(get()) }
    viewModel { ProfileUserViewModel(profileRepository = get()) }

    // ===== PROFILE CỦA CHÍNH MÌNH =====
    viewModel { ProfileUserViewModel(profileRepository = get()) }

    // ===== profile của CHÍNH MÌNH =====
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }

    // ===== TRIPS =====
    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get()) }
    viewModel { PlanSelectionViewModel(get()) }
    viewModel { PlanDetailViewModel(get()) }
    viewModel { TripMapViewModel(get()) }

    // ===== AI =====
    viewModel {
        AISuggestionViewModel(
            aiRepository = get(),
            tripRepository = get()
        )
    }
}

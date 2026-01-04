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
import com.datn.apptravel.data.remote.GroqService


val appModule = module {

    single { RetrofitClient.createService<ApiService>() }
    single { RetrofitClient.tripApiService }
    single { RetrofitClient.googleImageSearchService }
    single { DiscoverApiClient.api }
    single { DiscoverRepository(get()) }

    single { RetrofitClient.planMapApi }              // ← discoverRetrofit (8080)
    single { PlanMapDetailRepository(get()) }
    viewModel { PlanMapDetailViewModel(get(), get()) }

    single<FollowApi> { RetrofitClient.followApi }
    single { FollowRepository(get()) }

    single<ProfileApi> { RetrofitClient.profileApi }
    single { ProfileRepository(get()) }

    single { SessionManager(androidContext()) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single { AuthRepository() }
    single { UserRepository(get(), androidContext()) }
    single { NotificationRepository(get()) }
    single { PlacesRepository(get()) }
    single { TripRepository(get()) }
    single { ImageSearchRepository(get()) }

    single {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
    }

//    single {
//        get<Retrofit.Builder>()
//            .baseUrl("https://api.geoapify.com/")
//            .build()
//            .create(GeoapifyService::class.java)
//    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqService::class.java)
    }

    single {
        AIRepository(
            groqService = get(),
            imageSearchRepository = get()
        )
    }


    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { DiscoverViewModel(get(), sessionManager = get()) }
    viewModel { NotificationViewModel(get()) }
    viewModel { ProfileUserViewModel(profileRepository = get()) }

    viewModel { ProfileUserViewModel(profileRepository = get()) }

    viewModel { ProfileViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }

    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get(), get()) }
    viewModel { PlanSelectionViewModel(get()) }
    viewModel { PlanDetailViewModel(get()) }
    viewModel { TripMapViewModel(get()) }
}

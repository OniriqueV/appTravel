package com.datn.apptravels.di

import com.datn.apptravels.data.api.*
import com.datn.apptravels.data.local.SessionManager
import com.datn.apptravels.data.repository.*
import com.datn.apptravels.ui.app.*
import com.datn.apptravels.ui.discover.DiscoverViewModel
import com.datn.apptravels.ui.discover.PlanMap.PlanMapDetailViewModel
import com.datn.apptravels.ui.discover.network.*
import com.datn.apptravels.ui.discover.network.ProfileRepository
import com.datn.apptravels.ui.discover.profileFollow.ProfileUserViewModel
import com.datn.apptravels.ui.notification.NotificationViewModel
import com.datn.apptravels.ui.profile.ProfileViewModel
import com.datn.apptravels.ui.profile.edit.EditProfileViewModel
import com.datn.apptravels.ui.profile.password.ChangePasswordViewModel
import com.datn.apptravels.ui.trip.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.datn.apptravels.data.remote.GroqService


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

    single { NotificationRepository(get()) }
    single { AuthRepository(get()) }
    single { UserRepository(get(), androidContext()) }
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

    viewModel { TripsViewModel(get(), get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get(), get()) }
    viewModel { PlanSelectionViewModel(get()) }
    viewModel { PlanDetailViewModel(get()) }
    viewModel { TripMapViewModel(get()) }
}

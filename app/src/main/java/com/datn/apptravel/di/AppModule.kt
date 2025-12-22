package com.datn.apptravel.di

import com.datn.apptravel.data.api.ApiService
import com.datn.apptravel.data.api.RetrofitClient
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.data.repository.UserRepository
import com.datn.apptravel.data.repository.NotificationRepository
import com.datn.apptravel.ui.discover.network.ProfileApi
import com.datn.apptravel.ui.discover.profileFollow.ProfileRepository

import com.datn.apptravel.ui.app.*
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.notification.NotificationViewModel

import com.datn.apptravel.ui.profile.ProfileViewModel
import com.datn.apptravel.ui.profile.edit.EditProfileViewModel
import com.datn.apptravel.ui.profile.password.ChangePasswordViewModel

import com.datn.apptravel.ui.trip.viewmodel.*

import com.datn.apptravel.ui.discover.network.*
import com.datn.apptravel.ui.discover.profileFollow.ProfileUserViewModel as OtherProfileViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ================= API =================
    single { RetrofitClient.createService<ApiService>() }
    single { RetrofitClient.tripApiService }
    single { RetrofitClient.googleImageSearchService }

    // ================= DISCOVER =================
    single { DiscoverApiClient.api }
    single { DiscoverRepository(get()) }

    // ================= FOLLOW =================
    single<FollowApi> { RetrofitClient.followApi }
    single { FollowRepository(get()) }

    // ================= PROFILE (USER KHÁC) =================
    single<ProfileApi> { RetrofitClient.profileApi }
    single { ProfileRepository(get()) }

    viewModel {
        OtherProfileViewModel(
            profileRepository = get()
        )
    }

    // ================= LOCAL / FIREBASE =================
    single { SessionManager(androidContext()) }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // ================= OTHER REPOS =================
    single { AuthRepository() }
    single { UserRepository(get(), androidContext()) }
    single { NotificationRepository(get()) }
    single { com.datn.apptravel.data.repository.PlacesRepository(get()) }
    single { com.datn.apptravel.data.repository.TripRepository(get()) }
    single { com.datn.apptravel.data.repository.ImageSearchRepository(get()) }
    // ===== PROFILE FOLLOW (USER KHÁC) =====
    single<ProfileApi> { RetrofitClient.profileApi }
    single { ProfileRepository(get()) }

    viewModel {
        OtherProfileViewModel(
            profileRepository = get()
        )
    }

    // ================= VIEWMODELS =================
    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { DiscoverViewModel(get(), sessionManager = get()) }
    viewModel { NotificationViewModel(get()) }

    // ===== profile của CHÍNH MÌNH =====
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }

    // ===== trips =====
    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get()) }
    viewModel { PlanSelectionViewModel(get()) }
    viewModel { PlanDetailViewModel(get()) }
    viewModel { TripMapViewModel(get()) }
}

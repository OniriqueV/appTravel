package com.datn.apptravel.di

import com.datn.apptravel.data.api.ApiService
import com.datn.apptravel.data.api.RetrofitClient
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.app.SplashViewModel
import com.datn.apptravel.ui.app.MainViewModel
import com.datn.apptravel.ui.discover.DiscoverViewModel
import com.datn.apptravel.ui.notification.NotificationViewModel
import com.datn.apptravel.ui.profile.ProfileViewModel
import com.datn.apptravel.data.repository.UserRepository
import com.datn.apptravel.ui.profile.edit.EditProfileViewModel
import com.datn.apptravel.ui.profile.password.ChangePasswordViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.datn.apptravel.ui.trip.viewmodel.TripsViewModel
import com.datn.apptravel.ui.app.AuthViewModel
import com.datn.apptravel.ui.trip.viewmodel.CreateTripViewModel
import com.datn.apptravel.ui.trip.viewmodel.TripDetailViewModel
import com.datn.apptravel.ui.trip.viewmodel.PlanSelectionViewModel
import com.datn.apptravel.ui.trip.viewmodel.PlanDetailViewModel
import com.datn.apptravel.ui.trip.viewmodel.TripMapViewModel
import com.datn.apptravel.ui.app.OnboardingViewModel

import com.datn.apptravel.ui.discover.network.DiscoverApiClient
import com.datn.apptravel.ui.discover.network.DiscoverRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // API Service
    single { RetrofitClient.createService<ApiService>() }
    single { RetrofitClient.tripApiService }
    single { RetrofitClient.googleImageSearchService }

    // Local storage
    single { SessionManager(androidContext()) }

    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // Repositories
    single { AuthRepository() }
    single { UserRepository(get(), androidContext()) }
    single { com.datn.apptravel.data.repository.PlacesRepository(get()) }
    single { com.datn.apptravel.data.repository.TripRepository(get()) }
    single { DiscoverApiClient.api }
    single { DiscoverRepository(get()) }
    single { com.datn.apptravel.data.repository.ImageSearchRepository(get()) }
    single { com.datn.apptravel.data.repository.NotificationRepository(get()) }


    // ViewModels
    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { DiscoverViewModel(get()) }
    viewModel { NotificationViewModel(get()) }

    // Profile
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { EditProfileViewModel(get(), get()) }
    viewModel { ChangePasswordViewModel(get()) }

    // Trip
    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get()) }
    viewModel { PlanSelectionViewModel(get()) }
    viewModel { PlanDetailViewModel(get()) }
    viewModel { TripMapViewModel(get()) }
}
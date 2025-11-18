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
import com.datn.apptravel.ui.trip.viewmodel.TripsViewModel
import com.datn.apptravel.ui.auth.AuthViewModel
import com.datn.apptravel.ui.trip.viewmodel.CreateTripViewModel
import com.datn.apptravel.ui.trip.viewmodel.TripDetailViewModel
import com.datn.apptravel.ui.trip.viewmodel.PlanViewModel
import com.datn.apptravel.ui.auth.OnboardingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
val appModule = module {
    // API Service
    single { RetrofitClient.createService<ApiService>() }
    single { RetrofitClient.tripApiService }
    
    // Local storage
    single { SessionManager(androidContext()) }
    
    // Repositories - Firebase
    single { AuthRepository() }
    single { com.datn.apptravel.data.repository.PlacesRepository(get()) }
    single { com.datn.apptravel.data.repository.TripRepository(get()) }

    
    // ViewModels
    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { DiscoverViewModel() }
    viewModel { NotificationViewModel() }
    viewModel { ProfileViewModel(get()) }
    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get()) }
    viewModel { PlanViewModel(get()) }
}
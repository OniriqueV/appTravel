package com.datn.apptravel.di

import com.datn.apptravel.data.api.ApiService
import com.datn.apptravel.data.api.RetrofitClient
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.data.repository.AuthRepository
import com.datn.apptravel.ui.viewmodel.SplashViewModel
import com.datn.apptravel.ui.viewmodel.MainViewModel
import com.datn.apptravel.ui.viewmodel.GuidesViewModel
import com.datn.apptravel.ui.viewmodel.NotificationViewModel
import com.datn.apptravel.ui.viewmodel.ProfileViewModel
import com.datn.apptravel.ui.viewmodel.TripsViewModel
import com.datn.apptravel.ui.viewmodel.AuthViewModel
import com.datn.apptravel.ui.viewmodel.CreateTripViewModel
import com.datn.apptravel.ui.viewmodel.TripDetailViewModel
import com.datn.apptravel.ui.viewmodel.PlanViewModel
import com.datn.apptravel.ui.viewmodel.OnboardingViewModel
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
    viewModel { GuidesViewModel() }
    viewModel { NotificationViewModel() }
    viewModel { ProfileViewModel(get()) }
    viewModel { TripsViewModel(get(), get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { CreateTripViewModel(get(), get()) }
    viewModel { TripDetailViewModel(get()) }
    viewModel { PlanViewModel(get()) }
}
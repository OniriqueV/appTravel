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
import com.datn.apptravel.ui.viewmodel.TripViewModel
import com.datn.apptravel.ui.viewmodel.TripDetailViewModel
import com.datn.apptravel.ui.viewmodel.PlanViewModel
import com.datn.apptravel.ui.viewmodel.FlightViewModel
import com.datn.apptravel.ui.viewmodel.LodgingViewModel
import com.datn.apptravel.ui.viewmodel.BoatViewModel
import com.datn.apptravel.ui.viewmodel.TrainViewModel
import com.datn.apptravel.ui.viewmodel.OnboardingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
val appModule = module {
    // API Service
    single { RetrofitClient.createService<ApiService>() }
    
    // Local storage
    single { SessionManager(androidContext()) }
    
    // Repositories
    single { com.datn.apptravel.data.repository.PlacesRepository(get()) }
    single { AuthRepository(androidContext()) }
    
    // ViewModels
    viewModel { SplashViewModel(get()) }
    viewModel { OnboardingViewModel() }
    viewModel { MainViewModel(get()) }
    viewModel { GuidesViewModel() }
    viewModel { NotificationViewModel() }
    viewModel { ProfileViewModel(get()) }
    viewModel { TripsViewModel() }
    viewModel { AuthViewModel(get()) }
    viewModel { TripViewModel() }
    viewModel { TripDetailViewModel() }
    viewModel { PlanViewModel(get()) }
    viewModel { FlightViewModel() }
    viewModel { LodgingViewModel() }
    viewModel { BoatViewModel() }
    viewModel { TrainViewModel() }
}
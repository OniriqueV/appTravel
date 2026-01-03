//package com.datn.apptravel.ui.discover.PlanMap
//
//import android.app.Application
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//
//class TripSharedViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val _tripLikeDelta = MutableLiveData<Pair<String, Int>>()
//    val tripLikeDelta: LiveData<Pair<String, Int>> = _tripLikeDelta
//
//    fun notifyTripLikeChanged(tripId: String, delta: Int) {
//        _tripLikeDelta.value = Pair(tripId, delta)
//    }
//}

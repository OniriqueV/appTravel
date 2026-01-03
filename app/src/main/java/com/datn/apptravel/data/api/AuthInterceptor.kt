package com.datn.apptravel.data.api

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        // If user is authenticated and request doesn't already have X-User-Id header
        val request = if (currentUser != null && originalRequest.header("X-User-Id") == null) {
            originalRequest.newBuilder()
                .addHeader("X-User-Id", currentUser.uid)
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}

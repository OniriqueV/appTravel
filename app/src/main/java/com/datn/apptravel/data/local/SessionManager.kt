package com.datn.apptravels.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.datn.apptravels.data.model.Trip
import com.datn.apptravels.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// Cache data for discover trips
data class CachedDiscoverTripDetail(
    val trip: Trip,
    val user: User,
    val duration: Int,
    val startDateText: String,
    val durationText: String
)

class SessionManager(private val context: Context) {
    
    // In-memory cache for discover trips
    private val discoverTripCache = mutableMapOf<String, CachedDiscoverTripDetail>()
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val NOTIFICATIONS_ENABLED = stringPreferencesKey("notifications_enabled")
    }
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }
    
    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }
    
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }
    
    fun getUserId(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[USER_ID]
            }.first()
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled.toString()
        }
    }
    
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED]?.toBoolean() ?: true // Default to enabled
    }
    
    fun getNotificationsEnabled(): Boolean {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[NOTIFICATIONS_ENABLED]?.toBoolean() ?: true
            }.first()
        }
    }
    
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        discoverTripCache.clear()
    }
    
    // Discover trip cache methods
    fun cacheDiscoverTripDetail(tripId: String, detail: CachedDiscoverTripDetail) {
        discoverTripCache[tripId] = detail
    }
    
    fun getCachedDiscoverTripDetail(tripId: String): CachedDiscoverTripDetail? {
        return discoverTripCache[tripId]
    }
    
    fun clearDiscoverTripCache() {
        discoverTripCache.clear()
    }
}
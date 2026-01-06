package com.datn.apptravels.data.repository

import android.util.Log
import com.datn.apptravels.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS_COLLECTION = "users"
    }
    
    init {
        // Load current user on initialization
        auth.currentUser?.let { firebaseUser ->
            loadUserData(firebaseUser.uid)
        }
    }
    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<User> {
        return try {
            // Check if email already exists with different provider
            val existingUser = getUserByEmail(email)
            if (existingUser != null) {
                if (existingUser.provider == "GOOGLE") {
                    throw Exception("This email is already registered with Google Sign-In. Please use Google to login.")
                } else {
                    throw Exception("This email is already registered. Please login instead.")
                }
            }
            
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")
            
            // Create user document in Firestore
            val user = User(
                id = firebaseUser.uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                role = "user",
                profilePicture = null,
                provider = "LOCAL",
                providerId = null,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            saveUserToFirestore(user)
            _currentUser.value = user
            
            Log.d(TAG, "Sign up successful for user: ${user.email}")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Sign in with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to sign in")
            
            // Load user data from Firestore
            val user = getUserFromFirestore(firebaseUser.uid)
                ?: throw Exception("User data not found")
            
            _currentUser.value = user
            
            Log.d(TAG, "Login successful for user: ${user.email}")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            // Create credential with Google ID token
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            
            // Sign in with Firebase Auth
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to sign in with Google")
            
            // Check if email already registered with email/password
            val email = firebaseUser.email ?: throw Exception("No email from Google account")
            val existingUser = getUserByEmail(email)
            
            if (existingUser != null && existingUser.provider == "LOCAL") {
                // Email already registered with email/password
                auth.signOut() // Sign out from Google
                throw Exception("This email is already registered with Email/Password. Please use Email/Password to login.")
            }
            
            // Check if user already exists in Firestore
            var user = getUserFromFirestore(firebaseUser.uid)
            
            if (user == null) {
                // New user - create document in Firestore
                user = User(
                    id = firebaseUser.uid,
                    firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
                    lastName = firebaseUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                    email = email,
                    role = "user",
                    profilePicture = firebaseUser.photoUrl?.toString(),
                    provider = "GOOGLE",
                    providerId = firebaseUser.uid,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                saveUserToFirestore(user)
            } else {
                // Existing user - update last login time
                updateUserLastLogin(user.id)
            }
            
            _currentUser.value = user
            
            Log.d(TAG, "Google sign in successful for user: ${user!!.email}")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email", e)
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        return sendPasswordResetEmail(email)
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun logout() {
        try {
            auth.signOut()
            _currentUser.value = null
            Log.d(TAG, "Logout successful")
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
        }
    }
    suspend fun getAuthToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth token", e)
            null
        }
    }
    fun getUserData(): Flow<User?> {
        return currentUser
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(user.toMap())
                .await()
            Log.d(TAG, "User saved to Firestore: ${user.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user to Firestore", e)
            throw e
        }
    }

    private suspend fun getUserFromFirestore(userId: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user from Firestore", e)
            null
        }
    }

    private suspend fun getUserByEmail(email: String): User? {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user by email from Firestore", e)
            null
        }
    }

    private fun loadUserData(userId: String) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    _currentUser.value = user
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user data", e)
            }
    }

    private suspend fun updateUserLastLogin(userId: String) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("updatedAt", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last login", e)
        }
    }
}

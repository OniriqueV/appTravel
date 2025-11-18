package com.datn.apptravel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.R
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.ui.activity.MainActivity
import com.datn.apptravel.ui.auth.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignInActivity : AppCompatActivity() {
    
    private val viewModel: AuthViewModel by viewModel()
    private val sessionManager: SessionManager by inject()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        
        // Configure Google Sign-In
        // Use default_web_client_id from google-services.json
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Set up UI elements
        setupUI()
        
        // Observe sign in result
        viewModel.signInResult.observe(this) { result ->
            result.onSuccess { user ->
                // Save user ID to session
                lifecycleScope.launch {
                    sessionManager.saveUserId(user.id)
                    Log.d("SignInActivity", "User ID saved to session: ${user.id}")
                }
                Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
                // Navigate to main screen
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this, "Sign in failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe Google sign in result
        viewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess { user ->
                // Save user ID to session
                lifecycleScope.launch {
                    sessionManager.saveUserId(user.id)
                    Log.d("SignInActivity", "User ID saved to session: ${user.id}")
                }
                Toast.makeText(this, "Google sign in successful", Toast.LENGTH_SHORT).show()
                // Navigate to main screen
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this, "Google sign in failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupUI() {
        // Set up sign in button
        findViewById<Button>(R.id.btnSignIn)?.setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail)?.text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword)?.text.toString()
            
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Sign in with provided credentials
            viewModel.signIn(email, password)
        }
        
        // Set up Google sign in button
        findViewById<Button>(R.id.btnGoogleSignIn)?.setOnClickListener {
            signInWithGoogle()
        }
        
        // Set up sign up text
        findViewById<TextView>(R.id.tvSignUp)?.setOnClickListener {
            navigateToSignUp()
        }
        
        // Set up forgot password text
        findViewById<TextView>(R.id.tvForgotPassword)?.setOnClickListener {
            navigateToForgotPassword()
        }
    }
    
    private fun signInWithGoogle() {
        // Sign out first to force account picker to show every time
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
    
    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            
            if (idToken != null) {
                Log.d("SignInActivity", "Google ID Token received")
                viewModel.signInWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Failed to get ID token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("SignInActivity", "Google sign in failed with code: ${e.statusCode}", e)
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}
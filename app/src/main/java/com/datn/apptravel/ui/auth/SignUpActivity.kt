package com.datn.apptravel.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.datn.apptravel.R
import com.datn.apptravel.data.local.SessionManager
import com.datn.apptravel.ui.activity.MainActivity
import com.datn.apptravel.ui.auth.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SignUpActivity : AppCompatActivity() {
    
    private val viewModel: AuthViewModel by viewModel()
    private val sessionManager: SessionManager by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        
        // Set up UI elements
        setupUI()
        
        // Observe sign up result
        viewModel.signUpResult.observe(this) { result ->
            result.onSuccess { user ->
                // Save user ID to session
                lifecycleScope.launch {
                    sessionManager.saveUserId(user.id)
                    Log.d("SignUpActivity", "User ID saved to session: ${user.id}")
                }
                Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this, "Sign up failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupUI() {
        // Set up register button
        findViewById<Button>(R.id.btnRegister)?.setOnClickListener {
            val firstName = findViewById<EditText>(R.id.etFirstName)?.text.toString().trim()
            val lastName = findViewById<EditText>(R.id.etLastName)?.text.toString().trim()
            val email = findViewById<EditText>(R.id.etEmail)?.text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword)?.text.toString()
            val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)?.text.toString()
            val termsAccepted = findViewById<CheckBox>(R.id.cbTerms)?.isChecked ?: false
            
            // Validate inputs
            when {
                firstName.isEmpty() -> {
                    Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show()
                }
                lastName.isEmpty() -> {
                    Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show()
                }
                email.isEmpty() -> {
                    Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                }
                password.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                !termsAccepted -> {
                    Toast.makeText(this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Sign up with provided credentials
                    viewModel.signUp(firstName, lastName, email, password)
                }
            }
        }
        
        // Set up sign in text
        findViewById<TextView>(R.id.tvSignIn)?.setOnClickListener {
            finish() // Go back to sign in
        }
    }
    
    /**
     * Navigate to Main screen
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
package com.datn.apptravel.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.datn.apptravel.R
import com.datn.apptravel.ui.auth.AuthViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class ForgotPasswordActivity : AppCompatActivity() {
    
    private val viewModel: AuthViewModel by viewModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        
        // Setup UI
        setupUI()
        
        // Observe reset password result
        viewModel.resetPasswordResult.observe(this) { result ->
            result.fold(
                onSuccess = {
                    showResetSuccessMessage()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Failed to send reset email: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    private fun setupUI() {
        // Back button
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
        
        // Reset password button
        findViewById<Button>(R.id.btnForgotPassword)?.setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail)?.text.toString()
            
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.resetPassword(email)
            }
        }
        
        // Back to sign in link
        findViewById<TextView>(R.id.tvBackToSignIn)?.setOnClickListener {
            finish()
        }
    }
    
    /**
     * Show reset success message
     */
    private fun showResetSuccessMessage() {
        Toast.makeText(this, "Password reset instructions sent to your email!", Toast.LENGTH_LONG).show()
        finish()
    }
}
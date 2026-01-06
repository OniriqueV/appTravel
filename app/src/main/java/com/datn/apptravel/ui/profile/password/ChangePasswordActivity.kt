package com.datn.apptravels.ui.profile.password

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.datn.apptravels.databinding.ActivityChangePasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    private val viewModel: ChangePasswordViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Change password button
        binding.btnChangePassword.setOnClickListener {
            changePassword()
        }
    }

    private fun observeViewModel() {
        viewModel.changePasswordResult.observe(this) { success ->
            if (success) {
                showSuccessDialog()
            }
        }

        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                showToast(it)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnChangePassword.isEnabled = !isLoading
        }
    }

    private fun changePassword() {
        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Validate
        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            return
        }

        if (newPassword.isEmpty()) {
            binding.etNewPassword.error = "Vui lòng nhập mật khẩu mới"
            return
        }

        if (newPassword.length < 6) {
            binding.etNewPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }

        if (confirmPassword != newPassword) {
            binding.etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return
        }

        viewModel.changePassword(currentPassword, newPassword)
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Thành công")
            .setMessage("Mật khẩu đã được thay đổi thành công")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}